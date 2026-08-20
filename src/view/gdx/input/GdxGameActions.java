package view.gdx.input;

import data.GameDataManager;
import data.persistence.UserManager;
import model.account.User;
import model.core.GameManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;


/**
 * The graphical implementation of {@link GameActionBridge}: turns a click into the same
 * GameManager calls the terminal's GamePlayController makes, so planting rules, cooldowns and
 * plant levels behave identically in both builds. Nothing here is duplicated game logic -- it's
 * just the click-to-call plumbing GameActionBridge's javadoc asked for.
 *
 * <p>Messages go to System.out for now rather than a Toast, since this class isn't handed a
 * Stage/Skin. Wiring that up (so planting errors show on screen instead of the console) is a
 * natural next step once GameplayScreen wants it.
 */
public final class GdxGameActions implements GameActionBridge {

  private final GameManager match;
  private final Runnable onExit;

  public GdxGameActions(GameManager match, Runnable onExit) {
    this.match = match;
    this.onExit = onExit;
  }

  /** One simulation tick, for FixedStepClock to call every fixed interval. */
  public void advanceOneTick() {
    if (match != null && match.isRunning()) {
      match.advanceTime();
    }
  }

  @Override
  public void plantAt(int row, int column, String plantType) {
    if (match == null || plantType == null || GameDataManager.plantRepository == null) {
      return;
    }

    // Imitater has no behaviour of its own; it copies another plant already in the seed bank,
    // same as the terminal build (GamePlayController.resolveImitaterTarget).
    String buildType = plantType;
    if (plantType.equalsIgnoreCase("imitater")) {
      buildType = resolveImitaterTarget(plantType);
      if (buildType == null) {
        System.out.println("error: Imitater needs another plant in your seed bank to copy");
        return;
      }
    }

    PlantTemplate template = GameDataManager.plantRepository.find(buildType);
    if (template == null) {
      System.out.println("error: unknown plant '" + plantType + "'");
      return;
    }

    int level = currentPlantLevel(buildType);
    int remaining = match.ticksUntilPlantReady(plantType, adjustedRechargeSeconds(template, level));
    if (remaining > 0) {
      System.out.printf("error: %s is recharging; ready in %.1f seconds%n",
          plantType, remaining / 10.0);
      return;
    }

    Plant plant;
    try {
      plant = new PlantFactory(GameDataManager.plantRepository).createPlant(buildType, row, column, level);
    } catch (RuntimeException e) {
      System.out.println("error: could not build plant '" + plantType + "'");
      return;
    }
    if (plant == null) {
      System.out.println("error: unknown plant '" + plantType + "'");
      return;
    }

    if (match.placePlant(plant, row, column)) {
      match.recordPlanting(plantType);
    } else {
      System.out.println("error: cannot plant there (tile occupied or not enough sun)");
    }
  }

  @Override
  public void pluckAt(int row, int column) {
    Board board = board();
    if (board == null) {
      return;
    }
    Plant plant = board.getTopPlantAt(row, column);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    board.getPlants().remove(plant);
  }

  @Override
  public void collectSunAt(int row, int column) {
    if (match != null) {
      // GameManager takes (col, row); GameActionBridge takes (row, column) like Board.getTile.
      match.collectSunAt(column, row);
    }
  }

  @Override
  public void feedPlantAt(int row, int column) {
    Board board = board();
    if (board == null || match == null) {
      return;
    }
    Plant plant = board.getTopPlantAt(row, column);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    match.usePlantFood(plant);
  }

  @Override
  public void requestExit() {
    if (onExit != null) {
      onExit.run();
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
