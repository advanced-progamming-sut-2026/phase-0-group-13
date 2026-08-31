package view.gdx.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import view.gdx.core.GdxConfig;

public final class SeasonBackdrop {

  private record Layout(float mainWidth, float mainHeight, float leftWidth, float rightWidth,
      float gridX, float gridTop, float gridWidth, float gridHeight) {
  }

  private static final float SIDE_HEIGHT = 768f;

  private static final Layout EGYPT =
      new Layout(1024f, 768f, 278f, 673f, 253.6f, 199.6f, 735.6f, 488.5f);
  private static final Layout FROST =
      new Layout(1022f, 785f, 281f, 673f, 251.6f, 193.4f, 733.4f, 494.9f);
  private static final Layout BEACH =
      new Layout(1024f, 768f, 278f, 673f, 207.0f, 206.0f, 739.0f, 487.0f);
  private static final Layout DARK =
      new Layout(1024f, 768f, 278f, 673f, 251.3f, 201.4f, 746.2f, 487.2f);

  public static final String MAIN_REGION = "texture";
  private static final String LEFT_REGION = "texture_left";
  private static final String RIGHT_REGION = "texture_right";

  private SeasonBackdrop() {
  }

  private static Layout layoutFor(String seasonKey) {
    switch (seasonKey) {
      case "frost": return FROST;
      case "beach": return BEACH;
      case "dark": return DARK;
      default: return EGYPT;
    }
  }

  private static float scaleFor(Layout layout) {
    return GdxConfig.WORLD_HEIGHT / SIDE_HEIGHT;
  }

  public static float[] lawnBounds(String seasonKey) {
    return lawnBounds(layoutFor(seasonKey));
  }

  private static float[] lawnBounds(Layout layout) {
    float scale = scaleFor(layout);
    float width = layout.gridWidth() * scale;
    float height = layout.gridHeight() * scale;
    float x = (GdxConfig.WORLD_WIDTH - width) / 2f;
    float y = GdxConfig.WORLD_HEIGHT - (layout.gridTop() + layout.gridHeight()) * scale;
    return new float[] {x, y, width, height};
  }

  private static float mainLeft(Layout layout) {
    return lawnBounds(layout)[0] - layout.gridX() * scaleFor(layout);
  }

  /**
   * Draws the three strips, in one batch that is already begun.
   *
   * @return true if anything was drawn
   */
  public static boolean draw(Batch batch, TextureAtlas atlas, String seasonKey) {
    if (atlas == null) {
      return false;
    }
    TextureRegion main = atlas.findRegion(MAIN_REGION);
    if (main == null) {
      return false;
    }
    Layout layout = layoutFor(seasonKey);
    float scale = scaleFor(layout);
    float left = mainLeft(layout);
    float mainWidth = layout.mainWidth() * scale;
    float mainHeight = layout.mainHeight() * scale;
    float sideHeight = GdxConfig.WORLD_HEIGHT;

    TextureRegion leftStrip = atlas.findRegion(LEFT_REGION);
    if (leftStrip != null) {
      float stripWidth = layout.leftWidth() * scale;
      batch.draw(leftStrip, left - stripWidth, 0f, stripWidth, sideHeight);
    }
    batch.draw(main, left, GdxConfig.WORLD_HEIGHT - mainHeight, mainWidth, mainHeight);
    TextureRegion rightStrip = atlas.findRegion(RIGHT_REGION);
    if (rightStrip != null) {
      batch.draw(rightStrip, left + mainWidth, 0f, layout.rightWidth() * scale, sideHeight);
    }
    return true;
  }

  public static String atlasForSeason(String seasonKey) {
    switch (seasonKey) {
      case "egypt": return "textures/environment/ancientegyptseason.atlas";
      case "frost": return "textures/environment/frostbitecavesseason.atlas";
      case "beach": return "textures/environment/bigwavebeachseason.atlas";
      default: return "textures/environment/darkagesseason.atlas";
    }
  }
}
