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
  private static final float ZOMBIE_FOOT_INSET = 0.08f;
  private static final float PLANT_FOOT_INSET = 0.14f;

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
    drawSprites(context, board);
    drawShapes(context, board);
  }

  private void drawSprites(RenderContext context, Board board) {
    context.getBatch().begin();
    for (Plant plant : board.getPlants()) {
      TextureRegion art = plantArt.find(plant.getName());
      if (art != null) {
        drawStanding(context, art, plant.getCol(), plant.getRow(),
            scaleFor(art, PLANT_REFERENCE_HEIGHT, PLANT_ROW_FILL), PLANT_FOOT_INSET);
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      TextureRegion art = zombieArt.find(zombie.getName());
      if (art != null) {
        drawStanding(context, art, onBoard(zombie.getX()), zombie.getRow(),
            scaleFor(art, ZOMBIE_REFERENCE_HEIGHT, ZOMBIE_ROW_FILL), ZOMBIE_FOOT_INSET);
      }
    }
    TextureRegion pea = hudArt.find("pea");
    if (pea != null) {
      for (Projectile projectile : board.getProjectiles()) {
        drawCentred(context, pea, projectile.getXCoordinate(),
            Math.round(projectile.getYCoordinate()), geometry.getCellHeight() * 0.22f);
      }
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
    float width = region.getRegionWidth() * scale;
    float height = region.getRegionHeight() * scale;
    float bottom = geometry.rowToY(row) + geometry.getCellHeight() * footInset;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f, bottom, width, height);
  }

  private void drawCentred(RenderContext context, TextureRegion region, double col, double row,
      float targetHeight) {
    float scale = targetHeight / region.getRegionHeight();
    float width = region.getRegionWidth() * scale;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowCentreY(row) - targetHeight / 2f, width, targetHeight);
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
            geometry.rowCentreY(sun.getY()), 16f);
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
        : art.getRegionHeight() * scaleFor(art, ZOMBIE_REFERENCE_HEIGHT, ZOMBIE_ROW_FILL);
    float width = geometry.getCellWidth() * 0.55f;
    float x = geometry.columnCentreX(onBoard(zombie.getX())) - width / 2f;
    float y = geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + spriteHeight + 4f;
    // a tall zombie in the top lane would otherwise wear its bar up in the seed cards
    y = Math.min(y, geometry.rowToY(0) + geometry.getCellHeight() - 7f);
    float fraction = zombie.getCurrentHealth() / (float) Math.max(1, zombie.getMaxHealth());
    fraction = Math.max(0f, Math.min(1f, fraction));
    shapes.setColor(healthBack);
    shapes.rect(x - 1f, y - 1f, width + 2f, 7f);
    shapes.setColor(fraction < 0.35f ? healthLow : healthFront);
    shapes.rect(x, y, width * fraction, 5f);
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
  }
}
