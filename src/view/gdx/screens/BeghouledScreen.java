package view.gdx.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.ArrayList;
import java.util.List;
import model.enums.MiniGameType;
import model.game.minigame.arcade.BeghouledEngine;
import model.game.minigame.arcade.BeghouledEngine.PlantKind;
import model.game.minigame.arcade.BeghouledEngine.Upgrade;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

/**
 * Beghouled: the lawn as a match-three board.
 *
 * <p>{@link BeghouledEngine} owns all of it -- which plant sits where, what a swap is worth, when
 * the falling plants cascade, where the craters are and whether the target is met. This picks the
 * two tiles and draws the result.
 *
 * <p>Every plant is its own seed-packet portrait rather than the terminal build's letter, which is
 * what makes the board readable: matching is a game of spotting three alike, and three pictures
 * are far easier to spot than three copies of {@code CC}. Craters -- tiles a zombie has eaten and
 * nothing will ever grow on again -- get a gravestone, so a hole in the board reads as damage
 * rather than as a gap in the art.
 *
 * <p>A swap needs two clicks. The first arms a tile, the second either swaps with it or, when the
 * two are not neighbours, becomes the new armed tile: re-aiming is what the player meant, not an
 * error worth a toast. Clicking the armed tile again puts it down.
 */
public final class BeghouledScreen extends ArcadeBoardScreen {

  private static final float PLANT_ROW_FILL = 0.72f;
  private static final float CRATER_ROW_FILL = 0.66f;
  private static final Color CRATER_GROUND = new Color(0f, 0f, 0f, 0.34f);
  private static final Color ARMED = new Color(1f, 0.95f, 0.35f, 1f);
  private static final Color NEIGHBOUR = new Color(1f, 1f, 1f, 0.55f);
  private static final Color AFFORDABLE = new Color(1f, 1f, 1f, 1f);
  private static final Color TOO_DEAR = new Color(0.45f, 0.45f, 0.5f, 1f);

  private final BeghouledEngine engine;
  private final HudArt hudArt = new HudArt();
  private final List<SeedCard> upgradeCards = new ArrayList<>();
  private final List<Upgrade> upgradeOrder = new ArrayList<>();
  /** One per lane, so a zombie keeps its place in the walk cycle as it steps across. */
  private final Object[] zombieKeys = new Object[ROWS];
  /**
   * Portrait per plant kind, looked up once.
   *
   * <p>The whole board is redrawn every frame and every tile carries a picture, so going through
   * the atlas by name forty-five times a frame would mean forty-five string normalisations and
   * forty-five region scans for a set of eleven pictures that never changes.
   */
  private final TextureRegion[] portraits = new TextureRegion[PlantKind.values().length];
  private TextureRegion craterArt;

  private int armedRow = -1;
  private int armedColumn = -1;

  public BeghouledScreen(PvzGdxGame game, int level) {
    super(game, MiniGameType.BEGHOULED, level);
    this.engine = new BeghouledEngine(level);
    for (int row = 0; row < ROWS; row++) {
      zombieKeys[row] = new Object();
    }
  }

  @Override
  public void show() {
    // After super, which is what builds the renderer the atlases hang off.
    super.show();
    for (PlantKind kind : PlantKind.values()) {
      portraits[kind.ordinal()] = art.plantPortrait(kind.label);
    }
    craterArt = art.icon("gravestone");
  }

  @Override
  protected String seasonKey() {
    return "egypt";
  }

  @Override
  protected String title() {
    return "Beghouled  -  level " + getLevel();
  }

  @Override
  protected String statusLine() {
    return "sun " + engine.getSun()
        + "   -   matches " + engine.getMatchesMade() + "/" + engine.getMatchTarget()
        + (armedRow < 0
            ? "   -   click a plant, then a neighbour, to swap"
            : "   -   swapping (" + (armedColumn + 1) + ", " + (armedRow + 1)
                + ") - click a neighbour");
  }

  /** The upgrades the level offers, priced in sun. Same list the typed menu prints. */
  @Override
  protected void buildPicker(Table picker, Skin skin) {
    upgradeCards.clear();
    upgradeOrder.clear();
    picker.add(new Label("upgrades", skin, "secondary")).padRight(10f);
    for (Upgrade upgrade : engine.getUpgrades()) {
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, upgrade.from.label,
          upgrade.to.label, art.plantPortrait(upgrade.to.label), hudArt, this::buyUpgrade);
      card.withCost(upgrade.cost);
      picker.add(card).width(112f).height(126f).padRight(4f);
      upgradeCards.add(card);
      upgradeOrder.add(upgrade);
    }
    paintUpgrades();
  }

  /** Only affordability and the count on the lawn change, so the cards are repainted, not rebuilt. */
  @Override
  protected void refreshPicker() {
    paintUpgrades();
  }

  private void paintUpgrades() {
    for (int i = 0; i < upgradeCards.size() && i < upgradeOrder.size(); i++) {
      Upgrade upgrade = upgradeOrder.get(i);
      int onLawn = countOnLawn(upgrade.from);
      boolean affordable = engine.getSun() >= upgrade.cost && onLawn > 0;
      SeedCard card = upgradeCards.get(i);
      card.setStatus(onLawn == 0
          ? "no " + upgrade.from.label
          : onLawn + "x " + upgrade.from.label);
      card.setTint(affordable ? AFFORDABLE : TOO_DEAR);
    }
  }

  private int countOnLawn(PlantKind kind) {
    int count = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.getPlantAt(row, col) == kind) {
          count++;
        }
      }
    }
    return count;
  }

  private void buyUpgrade(String fromLabel) {
    toast(engine.upgrade(fromLabel));
    // An upgrade rewrites whole swathes of the board, so an armed tile may no longer mean anything.
    disarm();
    paintUpgrades();
  }

  @Override
  protected String onCellClicked(int row, int column) {
    if (engine.isCraterAt(row, column) || engine.getPlantAt(row, column) == null) {
      return armedRow < 0 ? null : "there is a crater there; nothing can move into it";
    }
    if (armedRow < 0) {
      armedRow = row;
      armedColumn = column;
      return null;
    }
    if (row == armedRow && column == armedColumn) {
      disarm();
      return null;
    }
    if (!isNeighbour(row, column)) {
      // Re-aim rather than refuse: a click on a far tile is a new pick, not a failed swap.
      armedRow = row;
      armedColumn = column;
      return null;
    }
    String result = engine.swap(armedRow, armedColumn, row, column);
    disarm();
    paintUpgrades();
    return result;
  }

  private boolean isNeighbour(int row, int column) {
    return Math.abs(row - armedRow) + Math.abs(column - armedColumn) == 1;
  }

  private void disarm() {
    armedRow = -1;
    armedColumn = -1;
  }

  @Override
  protected void tickEngine() {
    engine.tick();
    // A zombie can eat the very tile the player armed, which turns it into a crater. Drop the pick
    // rather than leave a highlight on a hole the next click could not swap with anyway.
    if (armedRow >= 0 && engine.getPlantAt(armedRow, armedColumn) == null) {
      disarm();
    }
  }

  @Override
  protected void drawWorld(float delta) {
    Batch batch = context().getBatch();
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.isCraterAt(row, col)) {
          art.drawProp(batch, craterArt, col, row, CRATER_ROW_FILL);
          continue;
        }
        PlantKind kind = engine.getPlantAt(row, col);
        if (kind != null) {
          art.drawProp(batch, portraits[kind.ordinal()], col, row, PLANT_ROW_FILL);
        }
      }
    }
    // The engine moves zombies a whole cell at a time and only reports them by tile, so the board
    // is scanned rather than a list walked; there is no sub-cell position to miss.
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.getZombieHealthAt(row, col) >= 0) {
          art.drawZombie(batch, zombieKeys[row], "zombie", col, row,
              engine.getPlantAt(row, col) != null);
        }
      }
    }
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    // The filled pass runs after the sprite batch, so this washes over the gravestone rather than
    // sitting behind it. That is the look wanted: a crater is a dead tile, and a stone in shadow
    // reads as dead where a brightly lit one would read as another piece to match.
    shapes.setColor(CRATER_GROUND);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.isCraterAt(row, col)) {
          shapes.rect(geometry.columnToX(col) + 1f, geometry.rowToY(row) + 1f,
              geometry.getCellWidth() - 2f, geometry.getCellHeight() - 2f);
        }
      }
    }
  }

  /** The armed tile and the tiles it can swap with, so the next click is never a guess. */
  @Override
  protected void drawOutlines(ShapeRenderer shapes) {
    if (armedRow < 0) {
      return;
    }
    shapes.setColor(ARMED);
    outlineCell(shapes, armedRow, armedColumn);
    shapes.setColor(NEIGHBOUR);
    outlineCell(shapes, armedRow - 1, armedColumn);
    outlineCell(shapes, armedRow + 1, armedColumn);
    outlineCell(shapes, armedRow, armedColumn - 1);
    outlineCell(shapes, armedRow, armedColumn + 1);
  }

  private void outlineCell(ShapeRenderer shapes, int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLUMNS || engine.isCraterAt(row, col)) {
      return;
    }
    shapes.rect(geometry.columnToX(col) + 2f, geometry.rowToY(row) + 2f,
        geometry.getCellWidth() - 4f, geometry.getCellHeight() - 4f);
  }

  @Override
  protected boolean engineFinished() {
    return engine.isFinished();
  }

  @Override
  protected boolean engineWon() {
    return engine.isWon();
  }

  @Override
  protected String outcomeWon() {
    return "You matched your way through " + engine.getMatchTarget()
        + " combos; the lawn is clear!";
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }
}
