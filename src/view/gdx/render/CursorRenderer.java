package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.core.GameManager;
import view.gdx.input.GameplayInputHandler;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;

public final class CursorRenderer implements WorldRenderer {

  private static final float PREVIEW_ALPHA = 0.65f;
  private static final float PREVIEW_ROW_FILL = 0.78f;
  private static final float PREVIEW_REFERENCE_HEIGHT = 70f;

  private final LawnGeometry geometry;
  private final GameplayInputHandler input;
  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();

  private final Color highlightFill = new Color(1f, 1f, 1f, 0.22f);
  private final Color highlightEdge = new Color(1f, 1f, 1f, 0.95f);
  private final Color shovelBody = new Color(0.72f, 0.55f, 0.30f, 0.95f);
  private final Color shovelHead = new Color(0.82f, 0.84f, 0.88f, 0.95f);

  public CursorRenderer(LawnGeometry geometry, GameplayInputHandler input) {
    this.geometry = geometry;
    this.input = input;
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null
        || input.getTool() == GameplayInputHandler.Tool.NONE) {
      return;
    }
    if (input.isHoveringLawn()) {
      drawCellHighlight(context);
    }
    drawCursorItem(context);
  }

  private void drawCellHighlight(RenderContext context) {
    float x = geometry.columnToX(input.getHoverColumn());
    float y = geometry.rowToY(input.getHoverRow());
    float width = geometry.getCellWidth();
    float height = geometry.getCellHeight();

    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(highlightFill);
    shapes.rect(x + 1f, y + 1f, width - 2f, height - 2f);
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(highlightEdge);
    shapes.rect(x + 1f, y + 1f, width - 2f, height - 2f);
    shapes.end();
  }

  private void drawCursorItem(RenderContext context) {
    switch (input.getTool()) {
      case SEED:
        drawPlantPreview(context);
        return;
      case PLANT_FOOD:
        drawIcon(context, hudArt.find("plantfood"));
        return;
      case SHOVEL:
        drawShovel(context);
        return;
      default:
    }
  }

  private void drawPlantPreview(RenderContext context) {
    TextureRegion art = plantArt.find(input.getSelectedPlantType());
    if (art == null) {
      return;
    }
    float scale = geometry.getCellHeight() * PREVIEW_ROW_FILL / PREVIEW_REFERENCE_HEIGHT;
    float widest = geometry.getCellWidth() * 0.95f;
    if (art.getRegionWidth() * scale > widest) {
      scale = widest / art.getRegionWidth();
    }
    drawGhost(context, art, art.getRegionWidth() * scale, art.getRegionHeight() * scale);
  }

  private void drawIcon(RenderContext context, TextureRegion art) {
    if (art == null) {
      return;
    }
    float height = geometry.getCellHeight() * 0.5f;
    drawGhost(context, art, art.getRegionWidth() * height / art.getRegionHeight(), height);
  }

  private void drawGhost(RenderContext context, TextureRegion art, float width, float height) {
    context.getBatch().begin();
    context.getBatch().setColor(1f, 1f, 1f, PREVIEW_ALPHA);
    context.getBatch().draw(art, input.getPointerWorldX() - width / 2f,
        input.getPointerWorldY() - height / 2f, width, height);
    context.getBatch().setColor(Color.WHITE);
    context.getBatch().end();
  }

  private void drawShovel(RenderContext context) {
    float size = geometry.getCellHeight() * 0.42f;
    float x = input.getPointerWorldX();
    float y = input.getPointerWorldY();

    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(shovelBody);
    shapes.rect(x - size * 0.08f, y - size * 0.1f, size * 0.16f, size * 0.8f);
    shapes.setColor(shovelHead);
    shapes.triangle(x - size * 0.3f, y - size * 0.1f,
        x + size * 0.3f, y - size * 0.1f, x, y - size * 0.6f);
    shapes.end();
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    hudArt.dispose();
  }
}
