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
import view.gdx.ui.GridMotion;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

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
  /** What has just fallen, so the board is drawn settling rather than snapping. See GridMotion. */
  private final GridMotion motion = new GridMotion(ROWS, COLUMNS);
  private final List<SeedCard> upgradeCards = new ArrayList<>();
  private final List<Upgrade> upgradeOrder = new ArrayList<>();
  private final Object[] zombieKeys = new Object[ROWS];
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
      armedRow = row;
      armedColumn = column;
      return null;
    }
    String[][] before = snapshot();
    String result = engine.swap(armedRow, armedColumn, row, column);
    motion.observe(before, snapshot());
    disarm();
    paintUpgrades();
    return result;
  }

  /** The grid as labels, which is all {@link GridMotion} needs to work out what moved. */
  private String[][] snapshot() {
    String[][] cells = new String[ROWS][COLUMNS];
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        PlantKind kind = engine.isCraterAt(row, col) ? null : engine.getPlantAt(row, col);
        cells[row][col] = kind == null ? null : kind.label;
      }
    }
    return cells;
  }

  private boolean isNeighbour(int row, int column) {
    return Math.abs(row - armedRow) + Math.abs(column - armedColumn) == 1;
  }

  private void disarm() {
    armedRow = -1;
    armedColumn = -1;
  }

  @Override
  protected boolean clearSelection() {
    if (armedRow < 0) {
      return false;
    }
    disarm();
    return true;
  }

  @Override
  protected void tickEngine() {
    String[][] before = snapshot();
    engine.tick();
    motion.observe(before, snapshot());
    if (armedRow >= 0 && engine.getPlantAt(armedRow, armedColumn) == null) {
      disarm();
    }
  }

  @Override
  protected void drawWorld(float delta) {
    motion.advance(delta);
    Batch batch = context().getBatch();
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.isCraterAt(row, col)) {
          art.drawProp(batch, craterArt, col, row, CRATER_ROW_FILL);
          continue;
        }
        PlantKind kind = engine.getPlantAt(row, col);
        if (kind != null) {
          // Drawn above its own square while it is still coming down. A row index further up the
          // grid is further up the screen, so subtracting the lift is the fall.
          art.drawProp(batch, portraits[kind.ordinal()], col, row - motion.liftOf(row, col),
              PLANT_ROW_FILL);
        }
      }
    }
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
