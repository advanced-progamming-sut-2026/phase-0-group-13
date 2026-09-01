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
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.IZombieEngine.DefensePlant;
import model.game.minigame.arcade.IZombieEngine.DeployedZombie;
import model.game.minigame.arcade.IZombieEngine.ZombieSpec;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.ArcadeRenderer;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

public final class IZombieScreen extends ArcadeBoardScreen {

  private static final float BRAIN_ROW_FILL = 0.42f;
  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color UNAVAILABLE = new Color(0.45f, 0.45f, 0.5f, 1f);
  private static final Color EATEN_BRAIN = new Color(0.35f, 0.35f, 0.38f, 0.55f);

  private final IZombieEngine engine;
  private final HudArt hudArt = new HudArt();
  private final List<SeedCard> cards = new ArrayList<>();
  private final List<ZombieSpec> offered = new ArrayList<>();

  /** The held-zombie preview: faint, and keyed apart from any real zombie's playback clock. */
  private static final float GHOST_ALPHA = 0.55f;
  private static final Object GHOST_KEY = new Object();

  private String chosen;

  public IZombieScreen(PvzGdxGame game, int level) {
    super(game, MiniGameType.I_ZOMBIE, level);
    this.engine = new IZombieEngine(level);
  }

  @Override
  protected String seasonKey() {
    return "dark";
  }

  @Override
  protected String title() {
    return "I, Zombie  -  level " + getLevel();
  }

  @Override
  protected String statusLine() {
    return "zombie-sun " + engine.getZombieSun() + "   -   brains left "
        + engine.getBrainsRemaining() + "/" + IZombieEngine.BRAINS
        + (chosen == null
            ? "   -   pick a zombie, then a tile right of the red line"
            : "   -   placing " + chosen);
  }

  @Override
  protected void buildPicker(Table picker, Skin skin) {
    cards.clear();
    offered.clear();
    picker.add(new Label("deploy", skin, "secondary")).padRight(10f);
    for (ZombieSpec spec : engine.availableZombieTypes()) {
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, spec.name, spec.name,
          art.zombiePortrait(spec.name), hudArt, this::choose);
      card.withCost(spec.cost);
      picker.add(card).width(114f).height(128f).padRight(4f);
      cards.add(card);
      offered.add(spec);
    }
    paintCards();
  }

  @Override
  protected void refreshPicker() {
    paintCards();
  }

  private void paintCards() {
    for (int i = 0; i < cards.size() && i < offered.size(); i++) {
      ZombieSpec spec = offered.get(i);
      SeedCard card = cards.get(i);
      int recharge = engine.rechargeTicksLeft(spec.name);
      boolean affordable = engine.getZombieSun() >= spec.cost;
      boolean drawable = art.zombiePortrait(spec.name) != null;
      card.setStatus(statusFor(recharge, affordable, drawable));
      card.setSelected(spec.name.equals(chosen));
      card.setTint(recharge > 0 || !affordable ? UNAVAILABLE : READY);
    }
  }

  private static String statusFor(int rechargeTicks, boolean affordable, boolean drawable) {
    if (rechargeTicks > 0) {
      return String.format("recharging %.1fs", rechargeTicks / 10.0);
    }
    if (!affordable) {
      return "need sun";
    }
    return drawable ? "ready" : "ready (no art)";
  }

  private void choose(String type) {
    chosen = type.equals(chosen) ? null : type;
    paintCards();
  }

  @Override
  protected boolean clearSelection() {
    if (chosen == null) {
      return false;
    }
    chosen = null;
    paintCards();
    return true;
  }

  @Override
  protected void tickEngine() {
    engine.tick();
  }

  @Override
  protected String onCellClicked(int row, int column) {
    if (chosen == null) {
      return "pick a zombie from the row above first";
    }
    return engine.placeZombie(chosen, row, column);
  }

  @Override
  protected void drawWorld(float delta) {
    Batch batch = context().getBatch();
    for (int row = 0; row < ROWS; row++) {
      if (engine.isBrainAlive(row)) {
        art.drawBesideLane(batch, art.icon("brain"), row, BRAIN_ROW_FILL, LANE_PROP_GAP);
      }
    }
    for (DefensePlant plant : engine.getDefensePlants()) {
      art.drawPlant(batch, plant, plant.getName(), plant.getCol(), plant.getRow());
    }
    for (DeployedZombie zombie : engine.getDeployedZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      art.drawZombie(batch, zombie, zombie.getName(), Math.max(0.0, zombie.getColumn()),
          zombie.getRow(), zombie.isEating());
    }
    drawPlacementGhost(batch);
  }

  /**
   * The zombie waiting to be placed, drawn faded on the tile under the pointer.
   *
   * <p>The doc asks for the held zombie's idle animation to follow the cursor while placing; the
   * seed row showed which card was picked but the lawn gave no sign of it, so a player choosing a
   * zombie and then looking at the board had nothing to tell them what they were about to drop.
   */
  private void drawPlacementGhost(Batch batch) {
    if (chosen == null || hoverRow() < 0 || hoverColumn() < 0) {
      return;
    }
    batch.setColor(1f, 1f, 1f, GHOST_ALPHA);
    art.drawZombie(batch, GHOST_KEY, chosen, hoverColumn(), hoverRow(), false);
    batch.setColor(1f, 1f, 1f, 1f);
  }

  @Override
  protected boolean highlightsWholeRow() {
    return true;
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    drawRedLine(shapes, IZombieEngine.RED_LINE_COLUMN, true);
    for (int row = 0; row < ROWS; row++) {
      if (!engine.isBrainAlive(row)) {
        shapes.setColor(EATEN_BRAIN);
        shapes.rect(geometry.columnToX(0) - LANE_PROP_GAP - 28f,
            geometry.rowCentreY(row) - 5f, 26f, 10f);
      }
    }
    for (DefensePlant plant : engine.getDefensePlants()) {
      art.healthBar(shapes, plant.getCol(), plant.getRow(),
          plant.getHealth() / (float) plant.getMaxHealth(), 0.8f);
    }
    for (DeployedZombie zombie : engine.getDeployedZombies()) {
      if (!zombie.isDead()) {
        art.healthBar(shapes, Math.max(0.0, zombie.getColumn()), zombie.getRow(),
            zombie.getHealth() / (float) zombie.getMaxHealth(), 0.86f);
      }
    }
  }

  @Override
  protected void drawOutlines(ShapeRenderer shapes) {
    for (DeployedZombie zombie : engine.getDeployedZombies()) {
      if (!zombie.isDead() && ArcadeRenderer.lookOf(zombie.getName()) == null) {
        art.outline(shapes, Math.max(0.0, zombie.getColumn()), zombie.getRow());
      }
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
    return "All five brains eaten!";
  }

  @Override
  protected String outcomeLost() {
    return "No sun and nothing left on the lawn - the plants held.";
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }
}
