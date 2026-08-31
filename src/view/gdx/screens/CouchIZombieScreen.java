package view.gdx.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.enums.MatchRole;
import model.enums.MiniGameType;
import model.game.minigame.arcade.IZombieAction;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.IZombieEngine.PlantSpec;
import model.game.minigame.arcade.IZombieEngine.ZombieSpec;
import model.game.minigame.arcade.IZombieMatch;
import model.game.minigame.arcade.IZombieMatch.Snapshot;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

public final class CouchIZombieScreen extends SnapshotIZombieScreen {

  private static final float CARD_WIDTH = 92f;
  private static final float CARD_HEIGHT = 120f;

  private static final Color CURSOR = new Color(0.95f, 0.3f, 0.35f, 0.85f);
  private static final Color CURSOR_BLOCKED = new Color(0.45f, 0.45f, 0.5f, 0.6f);

  private final IZombieMatch match;
  private final HudArt hudArt = new HudArt();

  private final List<SeedCard> plantCards = new ArrayList<>();
  private final List<SeedCard> zombieCards = new ArrayList<>();
  private final List<PlantSpec> plantSpecs = new ArrayList<>();
  private final List<ZombieSpec> zombieSpecs = new ArrayList<>();

  private Snapshot snapshot;
  private String chosenPlant;
  private String chosenZombie;
  private int cursorRow = ROWS / 2;
  private int cursorColumn = COLUMNS - 1;

  public CouchIZombieScreen(PvzGdxGame game, int level) {
    super(game, MiniGameType.I_ZOMBIE, level);
    this.match = new IZombieMatch(level, System.nanoTime());
    this.snapshot = match.snapshot();
  }

  @Override
  protected Snapshot currentSnapshot() {
    return snapshot;
  }

  @Override
  protected String title() {
    return "I, Zombie couch play";
  }

  @Override
  protected String statusLine() {
    Snapshot state = snapshot;
    if (state == null) {
      return "starting...";
    }
    return "P1 sun " + state.plantSun() + "  |  P2 sun " + state.zombieSun()
        + "  |  brains " + state.brainsRemaining() + "/" + IZombieEngine.BRAINS
        + "  |  " + clock(state.ticksRemaining())
        + "  |  P2 aim r" + (cursorRow + 1) + "c" + (cursorColumn + 1) + " (1-9/WASD/Enter)";
  }

  @Override
  protected void buildPicker(Table picker, Skin skin) {
    plantCards.clear();
    zombieCards.clear();
    plantSpecs.clear();
    zombieSpecs.clear();

    picker.add(new Label("P1 mouse", skin, "secondary")).padRight(8f);
    for (PlantSpec spec : IZombieEngine.availablePlantTypes()) {
      plantSpecs.add(spec);
      plantCards.add(addCard(picker, skin, spec.name, spec.cost, art.plantPortrait(spec.name),
          this::choosePlant));
    }
    picker.add(new Label("P2 keys", skin, "secondary")).padLeft(12f).padRight(8f);
    int slot = 1;
    for (ZombieSpec spec : IZombieEngine.zombieTypesFor(getLevel())) {
      zombieSpecs.add(spec);
      // Labelled with the number key that arms it, since this player never uses the mouse.
      zombieCards.add(addCard(picker, skin, spec.name, spec.cost,
          art.zombiePortrait(spec.name), this::chooseZombie, slot++));
    }
    paintCards();
  }

  private SeedCard addCard(Table row, Skin skin, String name, int cost, TextureRegion portrait,
      java.util.function.Consumer<String> onPick) {
    return addCard(row, skin, name, cost, portrait, onPick, 0);
  }

  private SeedCard addCard(Table row, Skin skin, String name, int cost, TextureRegion portrait,
      java.util.function.Consumer<String> onPick, int slot) {
    String label = slot > 0 ? slot + ". " + name : name;
    SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, name, label, portrait, hudArt,
        onPick::accept);
    card.withCost(cost);
    row.add(card).width(CARD_WIDTH).height(CARD_HEIGHT).padRight(3f);
    return card;
  }

  private void choosePlant(String type) {
    chosenPlant = type.equals(chosenPlant) ? null : type;
    paintCards();
  }

  private void chooseZombie(String type) {
    chosenZombie = type.equals(chosenZombie) ? null : type;
    paintCards();
  }

  @Override
  protected boolean clearSelection() {
    if (chosenPlant == null && chosenZombie == null) {
      return false;
    }
    chosenPlant = null;
    chosenZombie = null;
    paintCards();
    return true;
  }

  @Override
  protected void refreshPicker() {
    paintCards();
  }

  private void paintCards() {
    Snapshot state = snapshot;
    paint(plantCards, plantNames(), state == null ? 0 : state.plantSun(), chosenPlant,
        state == null ? null : state.plantRecharge(), plantCosts());
    paint(zombieCards, zombieNames(), state == null ? 0 : state.zombieSun(), chosenZombie,
        state == null ? null : state.zombieRecharge(), zombieCosts());
  }

  private void paint(List<SeedCard> cards, List<String> names, int purse, String chosen,
      Map<String, Integer> recharge, List<Integer> costs) {
    for (int i = 0; i < cards.size() && i < names.size(); i++) {
      String name = names.get(i);
      Integer left = recharge == null ? null : recharge.get(name);
      int ticks = left == null ? 0 : left;
      boolean affordable = purse >= costs.get(i);
      SeedCard card = cards.get(i);
      card.setStatus(ticks > 0
          ? String.format("recharging %.1fs", ticks / (double) IZombieEngine.TICKS_PER_SECOND)
          : (affordable ? "ready" : "need sun"));
      card.setSelected(name.equals(chosen));
      card.setTint(ticks > 0 || !affordable ? UNAVAILABLE : READY);
    }
  }

  private List<String> plantNames() {
    List<String> names = new ArrayList<>();
    for (PlantSpec spec : plantSpecs) {
      names.add(spec.name);
    }
    return names;
  }

  private List<Integer> plantCosts() {
    List<Integer> costs = new ArrayList<>();
    for (PlantSpec spec : plantSpecs) {
      costs.add(spec.cost);
    }
    return costs;
  }

  private List<String> zombieNames() {
    List<String> names = new ArrayList<>();
    for (ZombieSpec spec : zombieSpecs) {
      names.add(spec.name);
    }
    return names;
  }

  private List<Integer> zombieCosts() {
    List<Integer> costs = new ArrayList<>();
    for (ZombieSpec spec : zombieSpecs) {
      costs.add(spec.cost);
    }
    return costs;
  }

  @Override
  protected void tickEngine() {
    match.tick();
    snapshot = match.snapshot();
  }

  @Override
  protected String onCellClicked(int row, int column) {
    if (chosenPlant == null) {
      return "P1: pick a plant first";
    }
    String refusal = match.apply(MatchRole.PLANTS,
        IZombieAction.placePlant(chosenPlant, row, column));
    if (refusal == null) {
      chosenPlant = null;
      snapshot = match.snapshot();
      paintCards();
    }
    return refusal;
  }

  @Override
  protected boolean onKeyPressed(int keycode) {
    if (isPaused()) {
      return false;
    }
    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
      return pickZombieSlot(keycode - Input.Keys.NUM_1);
    }
    switch (keycode) {
      case Input.Keys.W:
      case Input.Keys.UP:
        return moveCursor(-1, 0);
      case Input.Keys.S:
      case Input.Keys.DOWN:
        return moveCursor(1, 0);
      case Input.Keys.A:
      case Input.Keys.LEFT:
        return moveCursor(0, -1);
      case Input.Keys.D:
      case Input.Keys.RIGHT:
        return moveCursor(0, 1);
      case Input.Keys.ENTER:
        return placeZombie();
      default:
        return false;
    }
  }

  private boolean pickZombieSlot(int index) {
    if (index >= zombieSpecs.size()) {
      return false;
    }
    chooseZombie(zombieSpecs.get(index).name);
    return true;
  }

  private boolean moveCursor(int rows, int columns) {
    cursorRow = Math.max(0, Math.min(ROWS - 1, cursorRow + rows));
    cursorColumn = Math.max(0, Math.min(COLUMNS - 1, cursorColumn + columns));
    paintCards();
    return true;
  }

  private boolean placeZombie() {
    if (chosenZombie == null) {
      toast("P2: press a number key to pick a zombie");
      return true;
    }
    String refusal = match.apply(MatchRole.ZOMBIES,
        IZombieAction.placeZombie(chosenZombie, cursorRow, cursorColumn));
    if (refusal == null) {
      chosenZombie = null;
      snapshot = match.snapshot();
      paintCards();
    } else {
      toast(refusal);
    }
    return true;
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    super.drawOverlays(shapes);
    shapes.setColor(cursorColumn > IZombieEngine.RED_LINE_COLUMN ? CURSOR : CURSOR_BLOCKED);
    float x = geometry.columnToX(cursorColumn);
    float y = geometry.rowToY(cursorRow);
    shapes.rect(x + 2f, y + 2f, geometry.getCellWidth() - 4f, geometry.getCellHeight() - 4f);
  }

  @Override
  protected boolean engineFinished() {
    return match.isFinished();
  }

  @Override
  protected boolean engineWon() {
    return match.winner() == MatchRole.PLANTS;
  }

  @Override
  protected String outcomeWon() {
    return "P1 (plants) wins - the lawn held for " + clock(IZombieMatch.SURVIVAL_TICKS) + ".";
  }

  @Override
  protected String outcomeLost() {
    Snapshot state = snapshot;
    int eaten = state == null ? 0 : IZombieEngine.BRAINS - state.brainsRemaining();
    return "P2 (zombies) wins - " + eaten + " of " + IZombieEngine.BRAINS + " brains eaten.";
  }

  @Override
  protected void recordOutcome(boolean won) {
  }

  @Override
  protected String leaveButtonLabel() {
    return "Back to mini-games";
  }

  @Override
  protected void leave() {
    game.switchScreen(new MiniGamesScreen(game));
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }
}
