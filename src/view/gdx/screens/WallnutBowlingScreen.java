package view.gdx.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.ArrayList;
import java.util.List;
import model.enums.MiniGameType;
import model.game.minigame.arcade.WallnutBowlingEngine;
import model.game.minigame.arcade.WallnutBowlingEngine.LaneZombie;
import model.game.minigame.arcade.WallnutBowlingEngine.NutType;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

/**
 * Wall-nut Bowling: the belt hands you a nut, you drop it in a lane and it rolls.
 *
 * <p>{@link WallnutBowlingEngine} owns the belt, the rolling, the bounces and the collisions. The
 * screen never moves a nut itself: it asks the engine which nut is on which tile this tick and
 * draws it there, which is why a nut that the engine has spent stops being drawn in the same
 * frame it stops existing.
 *
 * <p>The three kinds are told apart by their own art. The bowling wall-nut is the plain nut and
 * the Explode-o-nut is the red one, both from their own seed packets; the giant is the wall-nut at
 * nearly twice the size, which is the only thing that distinguishes it in the source library too.
 */
public final class WallnutBowlingScreen extends ArcadeBoardScreen {

  private static final float NUT_ROW_FILL = 0.55f;
  private static final float GIANT_ROW_FILL = 0.92f;
  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color WAITING = new Color(0.4f, 0.4f, 0.46f, 1f);

  private final WallnutBowlingEngine engine;
  private final HudArt hudArt = new HudArt();
  private final List<SeedCard> cards = new ArrayList<>();

  private String shownReady = "";

  public WallnutBowlingScreen(PvzGdxGame game, int level) {
    super(game, MiniGameType.WALLNUT_BOWLING, level);
    this.engine = new WallnutBowlingEngine(level);
  }

  @Override
  protected String seasonKey() {
    return "egypt";
  }

  @Override
  protected String title() {
    return "Wall-nut Bowling  -  level " + getLevel();
  }

  @Override
  protected String statusLine() {
    return "score " + engine.getScore() + "   -   zombies left to send "
        + engine.getZombiesRemainingToSpawn() + "   -   plant in columns 1-"
        + (WallnutBowlingEngine.RED_LINE_COLUMN + 1);
  }

  /** The belt: all three kinds, with whatever it has just delivered lit up. */
  @Override
  protected void buildPicker(Table picker, Skin skin) {
    cards.clear();
    shownReady = engine.getReadyNutLabel();
    picker.add(new Label("belt", skin, "secondary")).padRight(10f);
    for (NutType nut : NutType.values()) {
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, nut.label, nut.label,
          art.plantPortrait(packetOf(nut)), hudArt, null);
      picker.add(card).width(114f).height(116f).padRight(4f);
      cards.add(card);
    }
    paintBelt();
  }

  @Override
  protected void refreshPicker() {
    if (!engine.getReadyNutLabel().equals(shownReady)) {
      shownReady = engine.getReadyNutLabel();
      paintBelt();
    }
  }

  private void paintBelt() {
    NutType[] all = NutType.values();
    for (int i = 0; i < cards.size() && i < all.length; i++) {
      boolean ready = all[i].label.equalsIgnoreCase(shownReady);
      cards.get(i).setStatus(ready ? "ready" : "queued");
      cards.get(i).setSelected(ready);
      cards.get(i).setTint(ready ? READY : WAITING);
    }
  }

  /** Which seed packet is this nut's picture. The giant has no art of its own, see the class doc. */
  private static String packetOf(NutType nut) {
    return nut == NutType.EXPLODE_O_NUT ? "Explode-o-nut" : "Wall-nut";
  }

  @Override
  protected void tickEngine() {
    engine.tick();
  }

  @Override
  protected String onCellClicked(int row, int column) {
    return engine.plantNut(row, column);
  }

  @Override
  protected void drawWorld(float delta) {
    Batch batch = context().getBatch();
    for (LaneZombie zombie : engine.getZombies()) {
      art.drawZombie(batch, zombie, "zombie", zombie.getColumn(), zombie.getLane(), false);
    }
    for (int lane = 0; lane < ROWS; lane++) {
      for (int col = 0; col < COLUMNS; col++) {
        NutType nut = engine.getNutTypeAt(lane, col);
        if (nut != null) {
          art.drawProp(batch, art.plantPortrait(packetOf(nut)), col, lane,
              nut == NutType.GIANT ? GIANT_ROW_FILL : NUT_ROW_FILL);
        }
      }
    }
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    drawRedLine(shapes, WallnutBowlingEngine.RED_LINE_COLUMN, true);
    for (LaneZombie zombie : engine.getZombies()) {
      art.healthBar(shapes, zombie.getColumn(), zombie.getLane(),
          zombie.getHealth() / (float) zombie.getMaxHealth(), 0.86f);
    }
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
    return "Every zombie bowled over - final score " + engine.getScore() + "!";
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }
}
