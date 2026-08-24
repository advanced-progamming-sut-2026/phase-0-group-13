package view.gdx.input;

import data.GameDataManager;
import data.persistence.UserManager;
import java.util.function.Consumer;
import model.account.User;
import model.core.GameManager;
import model.game.Board;
import model.game.Tile;
import model.game.minigame.ConveyorRule;
import model.game.minigame.SpecialStageRule;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;
import model.enums.PlantTag;


/**
 * The graphical implementation of {@link GameActionBridge}: turns a click into the same
 * GameManager calls the terminal's GamePlayController makes, so planting rules, cooldowns and
 * plant levels behave identically in both builds. Nothing here is duplicated game logic -- it's
 * just the click-to-call plumbing GameActionBridge's javadoc asked for.
 *
 * <p>Messages go to the notifier the screen hands in, normally the HUD toast, so a refusal shows
 * up on screen instead of in a console the player never sees.
 */
public final class GdxGameActions implements GameActionBridge {

  private final GameManager match;
  private final Runnable onExit;
  private final Consumer<String> notifier;

  public GdxGameActions(GameManager match, Runnable onExit, Consumer<String> notifier) {
    this.match = match;
    this.onExit = onExit;
    this.notifier = notifier;
  }

  /** One simulation tick, for FixedStepClock to call every fixed interval. */
  public void advanceOneTick() {
    if (match != null && match.isRunning()) {
      match.advanceTime();
    }
  }

  @Override
  public boolean plantAt(int row, int column, String plantType) {
    if (match == null || plantType == null || GameDataManager.plantRepository == null) {
      return false;
    }

    // Imitater has no behaviour of its own; it copies another plant already in the seed bank,
    // same as the terminal build (GamePlayController.resolveImitaterTarget).
    String buildType = plantType;
    if (plantType.equalsIgnoreCase("imitater")) {
      buildType = resolveImitaterTarget(plantType);
      if (buildType == null) {
        return report("Imitater needs another plant in your seed bank to copy.");
      }
    }

    PlantTemplate template = GameDataManager.plantRepository.find(buildType);
    if (template == null) {
      return report("Unknown plant '" + plantType + "'.");
    }

    int level = currentPlantLevel(buildType);
    int remaining = match.ticksUntilPlantReady(plantType, adjustedRechargeSeconds(template, level));
    if (remaining > 0) {
      return report(String.format("%s is recharging - ready in %.1fs.", plantType, remaining / 10.0));
    }

    Plant plant;
    try {
      plant = new PlantFactory(GameDataManager.plantRepository).createPlant(buildType, row, column, level);
    } catch (RuntimeException e) {
      return report("Could not build plant '" + plantType + "'.");
    }
    if (plant == null) {
      return report("Unknown plant '" + plantType + "'.");
    }

    if (!match.placePlant(plant, row, column)) {
      return report(explainRefusedPlanting(plant, row, column));
    }
    match.recordPlanting(plantType);
    // Same hand-off GamePlayController makes: the belt slot is spent, so the next click waits.
    if (match.getSpecialStageRule() instanceof ConveyorRule belt) {
      belt.consumeReadyPlant();
    }
    return true;
  }

  // Why placePlant said no. The model has already refused; this only re-reads the board in the
  // same order to name the reason, so the player gets a message instead of a dead click.
  private String explainRefusedPlanting(Plant plant, int row, int column) {
    Board board = board();
    if (board == null) {
      return "The match is not running.";
    }
    SpecialStageRule rule = match.getSpecialStageRule();
    if (rule != null && !rule.isPlantAllowed(plant.getName())) {
      return plant.getName() + " is locked in this stage.";
    }
    Tile tile = board.getTile(row, column);
    if (tile != null && tile.getEffect() != null && tile.getEffect().blocksPlanting()) {
      return "A " + tile.getEffect().getName() + " is in the way.";
    }
    boolean aquatic = plant.getTags().contains(PlantTag.WATER);
    boolean water = board.isWaterAt(row, column);
    if (aquatic && !water) {
      return plant.getName() + " can only go in the water.";
    }
    if (water && !aquatic) {
      return "You need a lily pad there before you can plant on water.";
    }
    if (board.getPlantAt(row, column) != null) {
      return "There is already a plant on that tile.";
    }
    if (!match.isFreePlanting() && match.getSunAmount() < plant.getCost()) {
      return "Not enough sun for " + plant.getName() + " (needs " + plant.getCost() + ").";
    }
    return "You cannot plant there.";
  }

  @Override
  public boolean pluckAt(int row, int column) {
    Board board = board();
    if (board == null) {
      return false;
    }
    Plant plant = board.getTopPlantAt(row, column);
    if (plant == null) {
      return report("No plant to dig up there.");
    }
    board.getPlants().remove(plant);
    notify(plant.getName() + " dug up.");
    return true;
  }

  @Override
  public boolean collectSunAt(int row, int column) {
    if (match == null) {
      return false;
    }
    // GameManager takes (col, row), this interface takes (row, column) like Board.getTile.
    // Null means no sun there, which is how the click falls through to planting.
    return match.collectSunAt(column, row) != null;
  }

  @Override
  public boolean feedPlantAt(int row, int column) {
    Board board = board();
    if (board == null || match == null) {
      return false;
    }
    Plant plant = board.getTopPlantAt(row, column);
    if (plant == null) {
      return report("No plant to feed there.");
    }
    if (match.getPlantFoodCount() <= 0) {
      return report("You have no plant food.");
    }
    if (!match.usePlantFood(plant)) {
      return report(plant.getName() + " has no plant food effect.");
    }
    notify("Plant food used on " + plant.getName() + ".");
    return true;
  }

  @Override
  public void requestExit() {
    if (onExit != null) {
      onExit.run();
    }
  }

  /** Message + "didn't happen", in one statement. */
  private boolean report(String message) {
    notify(message);
    return false;
  }

  private void notify(String message) {
    if (notifier != null) {
      notifier.accept(message);
    }
  }

  private Board board() {
    return match == null ? null : match.getBoard();
  }

  private static String resolveImitaterTarget(String selfType) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return null;
    }
    for (String deckPlant : user.getSelectedDeck()) {
      if (deckPlant != null && !deckPlant.equalsIgnoreCase(selfType)) {
        return deckPlant;
      }
    }
    return null;
  }

  private static int currentPlantLevel(String plantType) {
    User user = UserManager.getInstance().getCurrentUser();
    return user == null ? 1 : Math.max(1, user.getPlantLevel(plantType));
  }

  private static int adjustedRechargeSeconds(PlantTemplate template, int level) {
    PlantLevel levelStats = PlantLevel.cumulative(template, level);
    return Math.max(0, template.recharge + levelStats.getCooldownDeltaSeconds());
  }
}
