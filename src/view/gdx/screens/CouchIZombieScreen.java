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

/**
 * Couch play: the two-player I, Zombie match on one machine, with no server involved.
 *
 * <p>The rules are not reimplemented. This runs the same {@link IZombieMatch} the server runs, and
 * every move goes through the same {@code apply(role, action)} door -- so the price, the recharge,
 * the red line, who may place what and how the match is won are decided in exactly one place for
 * both the networked game and this one. What is different here is only that the clock is local and
 * that both roles are being played in front of the same screen.
 *
 * <p>Controls are the ones the doc asks for: the plant player has the mouse, the zombie player has
 * the keyboard. They act at the same time and neither blocks the other -- a keyboard placement and
 * a mouse placement in the same frame are two independent calls into the match, and the match
 * refuses each on its own merits. The keyboard drives a cursor of its own so the zombie player is
 * never fighting the other player for the pointer.
 */
public final class CouchIZombieScreen extends SnapshotIZombieScreen {

  /**
   * Narrower than the networked screen's cards.
   *
   * <p>That screen shows one player's strip; this one shows both, nine cards across, and at the
   * usual 114 the row runs off the left edge of a 1280-wide window and eats the first card.
   */
  private static final float CARD_WIDTH = 92f;
  private static final float CARD_HEIGHT = 120f;

  /** The keyboard cursor, drawn for the zombie player so they can see where they are aiming. */
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
    // A fresh seed each sitting, the same way the server picks one per match.
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
    // Kept short: the header is one row with the title and the buttons, and a long status pushes
    // the title off the left of the window.
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

    // Both strips in ONE row. The picker hangs from the top of the screen above a lawn whose
    // bounds are fixed, so a second row of cards would reach down over the top lane -- the same
    // overlap HudToolsRowHeightTest exists to stop happening again in the adventure HUD.
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

  /**
   * Escape clears both players' picks.
   *
   * <p>There is only one keyboard between the two of them, so there is no way to tell which of them
   * pressed it. Dropping both is the answer that cannot leave someone holding a card they thought
   * they had put down; re-picking is one click or one key.
   */
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

  /** The local clock. One tick of the same match object the server would be ticking. */
  @Override
  protected void tickEngine() {
    match.tick();
    snapshot = match.snapshot();
  }

  /** The mouse is player one's, so a click on the lawn is always a plant. */
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

  /**
   * Player two's whole interface.
   *
   * <p>Consumes only the keys it actually uses, so Escape still leaves and Space still pauses.
   * Placing is Enter rather than Space for that reason.
   */
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

  /** The board, then player two's cursor over it so they can see where Enter would land. */
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

  /**
   * Both players are here, so there is no "you". The verdict panel says which side took it and
   * {@link #outcomeWon} spells it out; this only decides which of the two texts is shown.
   */
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

  /**
   * Nothing is banked from a couch match.
   *
   * <p>Two people shared one account's session, so neither result belongs to it: crediting the
   * signed-in player for a win they may not have played would put a score on the server's
   * leaderboard that nobody earned.
   */
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
