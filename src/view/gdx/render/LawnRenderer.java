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
import view.gdx.animation.AnimationLibrary;
import view.gdx.animation.AnimationStates;
import view.gdx.animation.EntityAnimation;
import view.gdx.core.GameSettings;
import view.gdx.ui.HudArt;

/** Background, lane grid and the tiles that block planting. */
public final class LawnRenderer implements WorldRenderer {

  private final LawnGeometry geometry;
  private final HudArt hudArt = new HudArt();
  private final Color gridLine = new Color(1f, 0.15f, 0.15f, 0.85f);
  private final Color graveColor = new Color(0.35f, 0.32f, 0.30f, 0.92f);
  /** How dark a grave gets just before it breaks. */
  private static final float GRAVE_WEAR_FLOOR = 0.45f;
  private final Color graveWear = new Color();
  /** Frame-to-frame health watch, only so a grave can flash on the tick it is hit. */
  private final HitEffects graveHits = new HitEffects();
  private final Color waterTile = new Color(0.16f, 0.55f, 0.72f, 0.45f);
  private final Color frozenTile = new Color(0.75f, 0.93f, 1f, 0.55f);
  private final Color slipTile = new Color(0.6f, 0.88f, 1f, 0.3f);
  private final Color slipArrow = new Color(0.15f, 0.45f, 0.7f, 0.85f);
  private final Color mowerReady = new Color(0.75f, 0.75f, 0.78f, 0.95f);
  private final Color mowerUsed = new Color(0.35f, 0.35f, 0.35f, 0.6f);

  /** Each world has its own mower rig under animations/effects; kind is "effects". */
  private static final String MOWER_KIND = "effects";
  /** On-screen mower height, as a fraction of a lane. Same size the still art was drawn at. */
  private static final float MOWER_HEIGHT_FRACTION = 0.55f;
  /**
   * The white star the original game throws when a mower launches.
   *
   * <p>A mower spending itself was previously a silent state change -- the sprite dimmed and that
   * was the whole event, for the thing that just saved the player's lane. This is that world's own
   * flash, out of the same HUD sheet the mower's own art comes from.
   */
  private static final String MOWER_FLASH_REGION = "whiteburst";
  private static final float MOWER_FLASH_SIZE_LANES = 1.5f;
  private static final String MOWER_SPARK = "mower";

  private final AnimationLibrary animations = new AnimationLibrary();
  private final AnimationStates playback = new AnimationStates();

  private TextureAtlas backgroundAtlas;
  private TextureRegion background;
  private String loadedFor;
  // Which season is on screen, so the props can pick the season's own art. Set every frame in
  // render() rather than threaded through every draw call.
  private String season = "egypt";
  private float clock;
  /** This frame's delta, so the props can drive their own clips without threading it through. */
  private float frameDelta;

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
    frameDelta = delta;
    watchGraves(game.getBoard(), delta);
    watchMowers(game.getBoard());
    drawBackground(context, game);
    drawGrid(context, game.getBoard());
    playback.endFrame();
  }

  /**
   * Backdrop only, for a screen that has a lawn but no match behind it.
   *
   * <p>The arcade mini-games run their own engines rather than a {@link GameManager}, so they
   * cannot go through {@link #render}, but the lawn they play on is the same lawn.
   */
  public void renderBackdrop(RenderContext context, String seasonKey, float delta) {
    season = seasonKey;
    clock += delta;
    drawBackground(context, atlasForSeason(seasonKey));
  }

  private void drawBackground(RenderContext context, GameManager game) {
    drawBackground(context, atlasForSeason(seasonKey(game)));
  }

  private void drawBackground(RenderContext context, String path) {
    if (!path.equals(loadedFor)) {
      if (backgroundAtlas != null) {
        backgroundAtlas.dispose();
      }
      backgroundAtlas = Gdx.files.internal(path).exists()
          ? new TextureAtlas(Gdx.files.internal(path)) : null;
      background = backgroundAtlas == null
          ? null : backgroundAtlas.findRegion(SeasonBackdrop.MAIN_REGION);
      loadedFor = path;
    }
    if (background != null) {
      context.getBatch().begin();
      SeasonBackdrop.draw(context.getBatch(), backgroundAtlas, season);
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

  private static String atlasForSeason(String seasonKey) {
    return SeasonBackdrop.atlasForSeason(seasonKey);
  }

  /** Where this match's lawn sits, straight off the world art. See {@link SeasonBackdrop}. */
  public static float[] lawnBounds(GameManager game) {
    return lawnBounds(seasonKey(game));
  }

  public static float[] lawnBounds(String seasonKey) {
    return SeasonBackdrop.lawnBounds(seasonKey);
  }

  private void drawGrid(RenderContext context, Board board) {
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
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
        shapes.rect(geometry.columnCentreX(mower.getX()) - size / 2f,
            geometry.rowCentreY(mower.getRow()) - size / 2f, size, size);
      }
    }
    shapes.end();
    drawGridLines(context, board);
    drawProps(context, board);
  }

  /** The Settings grid overlay. */
  private void drawGridLines(RenderContext context, Board board) {
    if (!GameSettings.isGridVisible()) {
      return;
    }
    float left = geometry.columnToX(0);
    float bottom = geometry.rowToY(board.getRows() - 1);
    float right = left + geometry.getCellWidth() * board.getColumns();
    float top = bottom + geometry.getCellHeight() * board.getRows();

    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(gridLine);
    for (int row = 0; row <= board.getRows(); row++) {
      float y = bottom + geometry.getCellHeight() * row;
      shapes.line(left, y, right, y);
    }
    for (int col = 0; col <= board.getColumns(); col++) {
      float x = left + geometry.getCellWidth() * col;
      shapes.line(x, bottom, x, top);
    }
    shapes.end();
  }

  /** Feeds every standing grave's health to the flash tracker before anything is drawn. */
  private void watchGraves(Board board, float delta) {
    graveHits.advance(delta);
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        if (board.getTile(row, col) == null) {
          continue;
        }
        if (board.getTile(row, col).getEffect() instanceof TombStoneEffect grave
            && grave.isActive()) {
          graveHits.observe(grave, grave.getHealth());
        }
      }
    }
    graveHits.endFrame(board.getColumns());
  }

  /**
   * Watches each mower's armed flag and flashes the one that has just gone off.
   *
   * <p>Armed counts as 1 and spent as 0, so the shared count watcher in {@link HitEffects} does
   * the whole job: it fires on the frame the number drops and never again.
   */
  private void watchMowers(Board board) {
    for (Lawnmower mower : board.getLawnmowers()) {
      graveHits.observeCount(mower, mower.isActive() ? 1 : 0, MOWER_SPARK,
          mower.getX(), mower.getRow());
    }
  }

  /** The flashes queued by {@link #watchMowers}, drawn over the mowers themselves. */
  private void drawMowerFlashes(RenderContext context) {
    TextureRegion flash = hudArt.find(MOWER_FLASH_REGION);
    if (flash == null) {
      return;
    }
    for (HitEffects.Spark spark : graveHits.getSparks()) {
      if (!MOWER_SPARK.equals(spark.kind())) {
        continue;
      }
      float size = geometry.getCellHeight() * MOWER_FLASH_SIZE_LANES
          * (0.5f + 0.5f * spark.progress());
      float width = flash.getRegionWidth() * size / flash.getRegionHeight();
      context.getBatch().setColor(1f, 1f, 1f, spark.alpha());
      context.getBatch().draw(flash, geometry.columnCentreX(spark.column()) - width / 2f,
          geometry.rowCentreY(spark.row()) - size / 2f, width, size);
    }
    context.getBatch().setColor(1f, 1f, 1f, 1f);
  }

  private void drawProps(RenderContext context, Board board) {
    EntityAnimation mowerRig = animations.find(MOWER_KIND, mowerKeyForSeason(season));
    TextureRegion mowerArt = mowerRig == null ? hudArt.find("lawnmower") : null;
    if (mowerRig == null && mowerArt == null && !hasGraveArt()
        && hudArt.find(MOWER_FLASH_REGION) == null) {
      return;
    }
    context.getBatch().begin();
    if (mowerRig != null) {
      drawMowers(context, board, mowerRig);
    } else if (mowerArt != null) {
      float height = geometry.getCellHeight() * MOWER_HEIGHT_FRACTION;
      float width = mowerArt.getRegionWidth() * height / mowerArt.getRegionHeight();
      for (Lawnmower mower : board.getLawnmowers()) {
        context.getBatch().setColor(1f, 1f, 1f, mower.isActive() ? 1f : 0.35f);
        context.getBatch().draw(mowerArt, geometry.columnCentreX(mower.getX()) - width / 2f,
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
    drawMowerFlashes(context);
    context.getBatch().end();
  }

  /**
   * The row mowers, in the rig the season ships.
   *
   * <p>Board.handleLawnmowers() clears the row the moment a zombie reaches it and never drives the
   * mower across, so there are only two states to draw: armed loops the rig's idle, spent holds
   * its fired pose. Position, size and the faded look of a spent mower are what the still art
   * used, so only the picture changes.
   */
  private void drawMowers(RenderContext context, Board board, EntityAnimation rig) {
    String idle = rig.pickClip("idle");
    String fired = rig.pickClip("attack", "transition", "idle");
    if (idle == null || fired == null) {
      return;
    }
    // One reference height for both clips, or the mower would resize as it changed state.
    float reference = rig.height(idle);
    if (reference <= 0f) {
      return;
    }
    float scale = geometry.getCellHeight() * MOWER_HEIGHT_FRACTION / reference;
    for (Lawnmower mower : board.getLawnmowers()) {
      String clip = mower.isActive() ? idle : fired;
      context.getBatch().setColor(1f, 1f, 1f, mower.isActive() ? 1f : 0.35f);
      rig.draw(context.getBatch(), clip, playback.advance(mower, clip, frameDelta),
          geometry.columnCentreX(mower.getX()),
          geometry.rowToY(mower.getRow()) + geometry.getCellHeight() * 0.12f, scale, false);
    }
    context.getBatch().setColor(1f, 1f, 1f, 1f);
  }

  /** The manifest key for this season's mower, matching assets/animations/effects. */
  private static String mowerKeyForSeason(String seasonKey) {
    switch (seasonKey) {
      case "frost": return "frostbitecavesseason";
      case "beach": return "bigwavebeachseason";
      case "dark": return "darkagesseason";
      default: return "ancientegyptseason";
    }
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
    // A grave is a destructible obstacle, so it has to look like one: it darkens and reddens as it
    // is worn down, and flashes white on the tick it takes a hit. Without this the player has no
    // way to tell a grave they are breaking from one they are wasting shots on.
    context.getBatch().setColor(graveTint(grave));
    context.getBatch().draw(art, geometry.columnCentreX(col) - width / 2f,
        geometry.rowToY(row) + geometry.getCellHeight() * 0.1f, width, height);
    context.getBatch().setColor(1f, 1f, 1f, 1f);
  }

  /** Darker and redder the closer the stone is to breaking, plus the hit flash over the top. */
  private Color graveTint(TombStoneEffect grave) {
    float left = Math.max(0f, Math.min(1f, grave.getHealth() / (float) grave.getMaxHealth()));
    // 1 at full health down to GRAVE_WEAR_FLOOR when it is about to break
    float wear = GRAVE_WEAR_FLOOR + (1f - GRAVE_WEAR_FLOOR) * left;
    graveWear.set(wear, wear * (0.55f + 0.45f * left), wear * (0.55f + 0.45f * left), 1f);
    float flash = graveHits.flashStrength(grave);
    return flash <= 0f ? graveWear : graveWear.lerp(Color.WHITE, flash);
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
    animations.dispose();
    if (backgroundAtlas != null) {
      backgroundAtlas.dispose();
      backgroundAtlas = null;
    }
  }
}
