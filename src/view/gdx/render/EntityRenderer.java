package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.core.GameManager;
import model.game.Board;
import model.game.Projectile;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.JesterZombieAction;
import model.game.zombie.behavior.KingAuraZombieAction;
import model.game.zombie.behavior.ZombossAction;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.ZombieArt;

/** Plants, zombies, projectiles and suns on top of the lawn. */
public final class EntityRenderer implements WorldRenderer {

  // Sprites are scaled off a reference height instead of being stretched to the tile, so a
  // gargantuar stays bigger than an imp and nothing gets distorted.
  private static final float ZOMBIE_REFERENCE_HEIGHT = 104f;
  private static final float ZOMBIE_ROW_FILL = 0.92f;
  private static final float PLANT_REFERENCE_HEIGHT = 70f;
  private static final float PLANT_ROW_FILL = 0.78f;
  // Zomboss's hit box in Zombies.json is 340 tall against a walker's 95, so it is not a
  // zombie-sized sprite: it stands about two and a half lanes high and is allowed to be wide.
  private static final float ZOMBOSS_ROW_FILL = 2.4f;
  private static final float ZOMBIE_FOOT_INSET = 0.08f;
  private static final float PLANT_FOOT_INSET = 0.14f;
  // Plants have no idle cycle of their own (see the .PAM note on ZombieArt); this tiny bob on
  // the footInset fraction is the whole stand-in the doc allows for plants, and it costs nothing
  // beyond what drawStanding already computes.
  private static final float PLANT_IDLE_SPEED = 2.2f;
  private static final float PLANT_IDLE_BOB_FRACTION = 0.02f;

  private final LawnGeometry geometry;
  private final PlantArt plantArt = new PlantArt();
  private final ZombieArt zombieArt = new ZombieArt();
  private final HudArt hudArt = new HudArt();
  private final Color healthBack = new Color(0f, 0f, 0f, 0.55f);
  private final Color healthFront = new Color(0.25f, 0.85f, 0.3f, 0.95f);
  private final Color healthLow = new Color(0.9f, 0.5f, 0.15f, 0.95f);
  private final Color peaColor = new Color(0.55f, 0.9f, 0.3f, 1f);
  private final Color sunColor = new Color(1f, 0.85f, 0.2f, 1f);
  private final Color noArt = new Color(1f, 1f, 1f, 0.85f);
  // Reflected shots belong to the zombie now, so they must not read as one of your peas.
  private final Color reflectedPea = new Color(1f, 0.42f, 0.3f, 1f);
  // King's aura pulse and the Juggler's spin both read this; it only ticks in render().
  private float clock;

  public EntityRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  public LawnGeometry getGeometry() {
    return geometry;
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null) {
      return;
    }
    Board board = game.getBoard();
    clock += delta;
    drawSprites(context, board);
    drawShapes(context, board);
  }

  private void drawSprites(RenderContext context, Board board) {
    context.getBatch().begin();
    TextureRegion sheep = hudArt.find("sheep");
    for (Plant plant : board.getPlants()) {
      // A Wizard's curse turns the plant into a harmless sheep until the wizard dies, so the
      // board has to show a sheep, not a plant that has quietly stopped shooting.
      boolean cursed = plant.isCursed() && sheep != null;
      TextureRegion art = cursed ? sheep : plantArt.find(plant.getName());
      if (art == null) {
        continue;
      }
      // the fleece is one effect frame, not a seed packet, so it is sized to the tile directly
      float scale = cursed
          ? geometry.getCellHeight() * PLANT_ROW_FILL / art.getRegionHeight()
          : scaleFor(art, PLANT_REFERENCE_HEIGHT, PLANT_ROW_FILL);
      drawStanding(context, art, plant.getCol(), plant.getRow(), scale,
          PLANT_FOOT_INSET + idleBobFraction(plant));
    }
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      drawKingAura(context, zombie);
      TextureRegion art = zombieArt.find(zombie.getName());
      if (art != null) {
        drawStanding(context, art, onBoard(zombie.getX()), zombie.getRow(),
            zombieScale(zombie, art), ZOMBIE_FOOT_INSET, spinAngle(zombie));
      }
    }
    TextureRegion pea = hudArt.find("pea");
    if (pea != null) {
      for (Projectile projectile : board.getProjectiles()) {
        // the juggler throws your own shots back at you; those have to look wrong
        context.getBatch().setColor(projectile.isFromZombie() ? reflectedPea : Color.WHITE);
        drawCentred(context, pea, projectile.getXCoordinate(),
            Math.round(projectile.getYCoordinate()), geometry.getCellHeight()
                * (projectile.isFromZombie() ? 0.28f : 0.22f));
      }
      context.getBatch().setColor(Color.WHITE);
    }
    TextureRegion sun = hudArt.find("sun");
    if (sun != null) {
      for (Sun s : board.getSuns()) {
        drawCentred(context, sun, s.getX(), s.getY(), geometry.getCellHeight() * 0.42f);
      }
    }
    context.getBatch().end();
  }

  /** A zombie past the mower still exists in the model; keep it on the lawn instead of off-screen.
   * Only the left side is clamped, zombies spawn to the right of the lawn and walk in. */
  private double onBoard(double column) {
    return Math.max(0.0, column);
  }

  /** Slow up/down drift, phase-shifted per tile so neighbouring plants do not bob in lockstep. */
  private float idleBobFraction(Plant plant) {
    float phase = (plant.getRow() * 3 + plant.getCol()) * 0.9f;
    return PLANT_IDLE_BOB_FRACTION * (float) Math.sin(clock * PLANT_IDLE_SPEED + phase);
  }

  private static boolean isBoss(Zombie zombie) {
    return zombie.getBehavior() instanceof ZombossAction;
  }

  /** Walkers share one reference height so a gargantuar stays bigger; Zomboss is its own size. */
  private float zombieScale(Zombie zombie, TextureRegion art) {
    return isBoss(zombie)
        ? geometry.getCellHeight() * ZOMBOSS_ROW_FILL / art.getRegionHeight()
        : scaleFor(art, ZOMBIE_REFERENCE_HEIGHT, ZOMBIE_ROW_FILL);
  }

  private float scaleFor(TextureRegion region, float referenceHeight, float rowFill) {
    float scale = geometry.getCellHeight() * rowFill / referenceHeight;
    // nothing may spill sideways into the neighbouring lane
    float widest = geometry.getCellWidth() * 0.95f;
    if (region.getRegionWidth() * scale > widest) {
      scale = widest / region.getRegionWidth();
    }
    return scale;
  }

  /** Feet on the ground rather than centred, so plants look planted and zombies look upright. */
  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset) {
    drawStanding(context, region, col, row, scale, footInset, 0f);
  }

  /** Same, but spun about its own middle. Only the juggler uses a non-zero angle. */
  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset, float angle) {
    float width = region.getRegionWidth() * scale;
    float height = region.getRegionHeight() * scale;
    float bottom = geometry.rowToY(row) + geometry.getCellHeight() * footInset;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f, bottom,
        width / 2f, height / 2f, width, height, 1f, 1f, angle);
  }

  /**
   * The juggler spins while shots keep coming at it and moves faster the whole time, and every
   * shot it catches goes back at the plants. Standing still while that happens would leave the
   * player with no idea why their peas turned around, so it actually spins on screen.
   */
  private float spinAngle(Zombie zombie) {
    return zombie.getBehavior() instanceof JesterZombieAction jester && jester.isSpinning()
        ? (clock * 540f) % 360f
        : 0f;
  }

  /**
   * The King never moves and never eats; what he does is speed up everything in his lane. That
   * is invisible unless the reach is drawn, so his own knighting burst marks it out.
   */
  private void drawKingAura(RenderContext context, Zombie zombie) {
    TextureRegion aura = hudArt.find("kingaura");
    if (aura == null || !(zombie.getBehavior() instanceof KingAuraZombieAction)) {
      return;
    }
    float size = geometry.getCellWidth() * 2.6f;
    float pulse = 0.32f + 0.12f * (float) Math.sin(clock * 2.5f);
    context.getBatch().setColor(1f, 0.9f, 0.45f, pulse);
    context.getBatch().draw(aura, geometry.columnCentreX(onBoard(zombie.getX())) - size / 2f,
        geometry.rowCentreY(zombie.getRow()) - size / 2f, size, size);
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawCentred(RenderContext context, TextureRegion region, double col, double row,
      float targetHeight) {
    float scale = targetHeight / region.getRegionHeight();
    float width = region.getRegionWidth() * scale;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowCentreY((int) Math.round(row)) - targetHeight / 2f, width, targetHeight);
  }

  private void drawShapes(RenderContext context, Board board) {
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);

    if (hudArt.find("pea") == null) {
      shapes.setColor(peaColor);
      for (Projectile projectile : board.getProjectiles()) {
        shapes.circle(geometry.columnCentreX(projectile.getXCoordinate()),
            geometry.rowCentreY(Math.round(projectile.getYCoordinate())), 7f);
      }
    }
    if (hudArt.find("sun") == null) {
      shapes.setColor(sunColor);
      for (Sun sun : board.getSuns()) {
        shapes.circle(geometry.columnCentreX(sun.getX()),
            geometry.rowCentreY((int) Math.round(sun.getY())), 16f);
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()) {
        healthBar(shapes, zombie);
      }
    }
    shapes.end();

    // A zombie with no verified portrait gets an outline rather than someone elses art.
    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(noArt);
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombieArt.find(zombie.getName()) == null) {
        shapes.rect(geometry.columnCentreX(onBoard(zombie.getX())) - geometry.getCellWidth() * 0.25f,
            geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * 0.1f,
            geometry.getCellWidth() * 0.5f, geometry.getCellHeight() * 0.7f);
      }
    }
    shapes.end();
  }

  /** Sits just above the sprite, so a tall zombie does not wear its bar on its chest. */
  private void healthBar(ShapeRenderer shapes, Zombie zombie) {
    TextureRegion art = zombieArt.find(zombie.getName());
    float spriteHeight = art == null
        ? geometry.getCellHeight() * 0.6f
        : art.getRegionHeight() * zombieScale(zombie, art);
    // the chapter ends when this one dies, so its bar has to be readable from across the lawn
    float width = geometry.getCellWidth() * (isBoss(zombie) ? 1.9f : 0.55f);
    float x = geometry.columnCentreX(onBoard(zombie.getX())) - width / 2f;
    float y = geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + spriteHeight + 4f;
    // a tall zombie in the top lane would otherwise wear its bar up in the seed cards
    y = Math.min(y, geometry.rowToY(0) + geometry.getCellHeight() - 7f);
    float fraction = zombie.getCurrentHealth() / (float) Math.max(1, zombie.getMaxHealth());
    fraction = Math.max(0f, Math.min(1f, fraction));
    float thickness = isBoss(zombie) ? 11f : 5f;
    shapes.setColor(healthBack);
    shapes.rect(x - 1f, y - 1f, width + 2f, thickness + 2f);
    shapes.setColor(fraction < 0.35f ? healthLow : healthFront);
    shapes.rect(x, y, width * fraction, thickness);
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
  }
}
