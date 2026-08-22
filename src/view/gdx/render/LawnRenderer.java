package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.core.GameManager;
import model.game.Board;
import model.game.Lawnmower;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TileEffect;
import model.game.TileEffects.TombStoneEffect;
import view.gdx.core.GdxConfig;
import view.gdx.ui.HudArt;

/** Background, lane grid and the tiles that block planting. */
public final class LawnRenderer implements WorldRenderer {

  private final LawnGeometry geometry;
  private final HudArt hudArt = new HudArt();
  private final Color lightLane = new Color(1f, 1f, 1f, 0.10f);
  private final Color darkLane = new Color(0f, 0f, 0f, 0.10f);
  private final Color graveColor = new Color(0.35f, 0.32f, 0.30f, 0.92f);
  private final Color waterTile = new Color(0.16f, 0.55f, 0.72f, 0.45f);
  private final Color frozenTile = new Color(0.75f, 0.93f, 1f, 0.55f);
  private final Color slipTile = new Color(0.6f, 0.88f, 1f, 0.3f);
  private final Color slipArrow = new Color(0.15f, 0.45f, 0.7f, 0.85f);
  private final Color mowerReady = new Color(0.75f, 0.75f, 0.78f, 0.95f);
  private final Color mowerUsed = new Color(0.35f, 0.35f, 0.35f, 0.6f);

  private TextureAtlas backgroundAtlas;
  private TextureRegion background;
  private String loadedFor;
  // Which season is on screen, so the props can pick the season's own art. Set every frame in
  // render() rather than threaded through every draw call.
  private String season = "egypt";
  private float clock;

  public LawnRenderer(LawnGeometry geometry) {
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
    season = seasonKey(game);
    clock += delta;
    drawBackground(context, game);
    drawGrid(context, game.getBoard());
  }

  private void drawBackground(RenderContext context, GameManager game) {
    String path = atlasFor(game);
    if (!path.equals(loadedFor)) {
      if (backgroundAtlas != null) {
        backgroundAtlas.dispose();
      }
      backgroundAtlas = Gdx.files.internal(path).exists()
          ? new TextureAtlas(Gdx.files.internal(path)) : null;
      background = backgroundAtlas == null ? null : backgroundAtlas.findRegion("texture");
      loadedFor = path;
    }
    if (background != null) {
      context.getBatch().begin();
      context.getBatch().draw(background, 0f, 0f, GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT);
      context.getBatch().end();
    }
  }

  private static String seasonKey(GameManager game) {
    String season = game == null || game.getSeason() == null
        ? "" : game.getSeason().getName().toLowerCase();
    if (season.contains("egypt")) {
      return "egypt";
    }
    if (season.contains("frost") || season.contains("cave")) {
      return "frost";
    }
    if (season.contains("beach") || season.contains("wave")) {
      return "beach";
    }
    return "dark";
  }

  private static String atlasFor(GameManager game) {
    switch (seasonKey(game)) {
      case "egypt": return "textures/environment/ancientegyptseason.atlas";
      case "frost": return "textures/environment/frostbitecavesseason.atlas";
      case "beach": return "textures/environment/bigwavebeachseason.atlas";
      default: return "textures/environment/darkagesseason.atlas";
    }
  }

  // Measured off each season's background art. Seasons we have not measured fall back to Egypt.
  public static float[] lawnBounds(GameManager game) {
    switch (seasonKey(game)) {
      case "frost": return new float[] {315.05f, 88.66f, 918.60f, 453.92f};
      case "beach": return new float[] {218.45f, 80.03f, 918.92f, 452.01f};
      case "dark": return new float[] {314.12f, 74.43f, 932.74f, 456.75f};
      default: return new float[] {317f, 74.85f, 919.44f, 458f};
    }
  }

  private void drawGrid(RenderContext context, Board board) {
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        shapes.setColor((row + col) % 2 == 0 ? lightLane : darkLane);
        shapes.rect(geometry.columnToX(col), geometry.rowToY(row),
            geometry.getCellWidth() - 1f, geometry.getCellHeight() - 1f);
        // the tide decides where you can plant, so it has to be visible
        if (board.isWaterAt(row, col)) {
          shapes.setColor(waterTile);
          shapes.rect(geometry.columnToX(col) + 1f, geometry.rowToY(row) + 1f,
              geometry.getCellWidth() - 2f, geometry.getCellHeight() - 2f);
        }
        drawTileEffect(shapes, board, row, col);
      }
    }
    if (hudArt.find("lawnmower") == null) {
      for (Lawnmower mower : board.getLawnmowers()) {
        shapes.setColor(mower.isActive() ? mowerReady : mowerUsed);
        float size = geometry.getCellHeight() * 0.32f;
        shapes.rect(geometry.columnToX(0) - size * 1.4f,
            geometry.rowCentreY(mower.getRow()) - size / 2f, size, size);
      }
    }
    shapes.end();
    drawProps(context, board);
  }

  private void drawProps(RenderContext context, Board board) {
    TextureRegion mowerArt = hudArt.find("lawnmower");
    if (mowerArt == null && !hasGraveArt()) {
      return;
    }
    context.getBatch().begin();
    if (mowerArt != null) {
      float height = geometry.getCellHeight() * 0.55f;
      float width = mowerArt.getRegionWidth() * height / mowerArt.getRegionHeight();
      for (Lawnmower mower : board.getLawnmowers()) {
        context.getBatch().setColor(1f, 1f, 1f, mower.isActive() ? 1f : 0.35f);
        context.getBatch().draw(mowerArt, geometry.columnToX(0) - width - 6f,
            geometry.rowToY(mower.getRow()) + geometry.getCellHeight() * 0.12f, width, height);
      }
      context.getBatch().setColor(1f, 1f, 1f, 1f);
    }
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        if (board.getTile(row, col) == null) {
          continue;
        }
        TileEffect effect = board.getTile(row, col).getEffect();
        if (effect instanceof TombStoneEffect grave && grave.isActive()) {
          drawGrave(context, grave, row, col);
        }
      }
    }
    context.getBatch().end();
  }

  /**
   * One grave, in the art of the season it stands in.
   *
   * <p>Dark Ages graves are not all the same thing and the doc says the player has to be able to
   * tell them apart: some have 50 sun buried in them, some a plant food, and some of the tiles
   * push a zombie out at the start of every wave. So the crest on the stone follows
   * TombStoneEffect.getBuriedReward() and the necromancy tiles get the world's own spawn disc
   * under them, pulsing so it reads as "something comes out of here".
   */
  private void drawGrave(RenderContext context, TombStoneEffect grave, int row, int col) {
    TextureRegion art = graveArt(grave);
    if (art == null) {
      return;
    }
    if (grave.isNecromancy()) {
      TextureRegion disc = hudArt.find("necromancy");
      if (disc != null) {
        float size = geometry.getCellWidth() * 0.86f;
        float pulse = 0.45f + 0.2f * (float) Math.sin(clock * 3f);
        context.getBatch().setColor(1f, 1f, 1f, pulse);
        context.getBatch().draw(disc, geometry.columnCentreX(col) - size / 2f,
            geometry.rowToY(row) + geometry.getCellHeight() * 0.06f, size, size);
        context.getBatch().setColor(1f, 1f, 1f, 1f);
      }
    }
    float height = geometry.getCellHeight() * 0.82f;
    float width = art.getRegionWidth() * height / art.getRegionHeight();
    context.getBatch().draw(art, geometry.columnCentreX(col) - width / 2f,
        geometry.rowToY(row) + geometry.getCellHeight() * 0.1f, width, height);
  }

  private boolean hasGraveArt() {
    return hudArt.find("dark".equals(season) ? "darkgrave" : "gravestone") != null;
  }

  /** Dark Ages has its own stone per buried reward; the other seasons share Egypt's. */
  private TextureRegion graveArt(TombStoneEffect grave) {
    if (!"dark".equals(season)) {
      return hudArt.find("gravestone");
    }
    if ("SUN".equals(grave.getBuriedReward())) {
      return hudArt.find("darkgravesun");
    }
    if ("PLANT_FOOD".equals(grave.getBuriedReward())) {
      return hudArt.find("darkgravefood");
    }
    return hudArt.find("darkgrave");
  }

  private void drawTileEffect(ShapeRenderer shapes, Board board, int row, int col) {
    if (board.getTile(row, col) == null) {
      return;
    }
    TileEffect effect = board.getTile(row, col).getEffect();
    if (effect instanceof IceTrailEffect ice && ice.isActive()) {
      drawIceTile(shapes, ice, row, col);
      return;
    }
    if (hasGraveArt()) {
      return;
    }
    if (effect instanceof TombStoneEffect grave && grave.isActive()) {
      shapes.setColor(graveColor);
      float width = geometry.getCellWidth() * 0.5f;
      float height = geometry.getCellHeight() * 0.55f;
      shapes.rect(geometry.columnCentreX(col) - width / 2f,
          geometry.rowCentreY(row) - height / 2f, width, height);
    }
  }

  // You cannot plant on ice, so it has to show. The arrow is the lane zombies slide to.
  private void drawIceTile(ShapeRenderer shapes, IceTrailEffect ice, int row, int col) {
    float x = geometry.columnToX(col);
    float y = geometry.rowToY(row);
    float w = geometry.getCellWidth() - 2f;
    float h = geometry.getCellHeight() - 2f;
    shapes.setColor(ice.isSlippery() ? slipTile : frozenTile);
    shapes.rect(x + 1f, y + 1f, w, h);
    if (!ice.isSlippery()) {
      return;
    }
    float cx = geometry.columnCentreX(col);
    float cy = geometry.rowCentreY(row);
    float size = Math.min(w, h) * 0.22f;
    float tip = ice.getLaneShift() > 0 ? cy - size : cy + size;
    shapes.setColor(slipArrow);
    shapes.triangle(cx - size, tip > cy ? cy - size * 0.4f : cy + size * 0.4f,
        cx + size, tip > cy ? cy - size * 0.4f : cy + size * 0.4f, cx, tip);
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    if (backgroundAtlas != null) {
      backgroundAtlas.dispose();
      backgroundAtlas = null;
    }
  }
}
