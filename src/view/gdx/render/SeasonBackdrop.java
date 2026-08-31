package view.gdx.render;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import view.gdx.core.GdxConfig;

/**
 * Lays out one world's backdrop and says where its painted lawn ends up.
 *
 * <p>Every {@code textures/environment/*.atlas} carries three regions, not one: {@code texture} is
 * the 4:3 view the art was drawn for, and {@code texture_left} / {@code texture_right} are the
 * strips the original game reveals either side of it on a wider screen. Only the middle one was
 * ever drawn, stretched from 1024x768 onto the 1280x720 world -- 33% wider than tall, which is why
 * a lawn tile came out wider than it is high when the artists drew it the other way round, and why
 * the whole scene read as squashed.
 *
 * <p>So this scales the three strips by one factor in both directions -- no distortion -- picks
 * the factor that makes the painting exactly fill the world's height, and slides the panorama
 * sideways until the painted grid sits where the board wants it. The strips then run past both
 * edges of the screen, so a 16:9 window is filled with the world's own scenery rather than with a
 * stretched middle. What is off-screen is off-screen; nothing is squeezed to fit.
 *
 * <p>The grid rectangles below were measured against each world's own art (a candidate 5x9 grid
 * overlaid on the source page and checked seam by seam), in the pixel space of that world's
 * {@code texture} region with y running down from its top edge. Everything the renderers and the
 * input handler need comes out of {@link #lawnBounds}, so the drawn lawn and the logical one
 * cannot drift apart.
 */
public final class SeasonBackdrop {

  /**
   * One world's page geometry and the painted grid inside it, in source pixels.
   *
   * <p>{@code mainHeight} is not always {@code SIDE_HEIGHT}: Frostbite's middle strip is 785 tall
   * where its sides are 768, which is why the three are anchored by their top edges rather than
   * stretched to a common height. Bottom-anchoring them instead puts a 17px step through the
   * mountain and the shoreline right where the side meets the middle.
   */
  private record Layout(float mainWidth, float mainHeight, float leftWidth, float rightWidth,
      float gridX, float gridTop, float gridWidth, float gridHeight) {
  }

  /** Every world's side strips are this tall; it is the height the whole panorama is scaled by. */
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

  /** How many world pixels one source pixel becomes. The same on both axes, which is the point. */
  private static float scaleFor(Layout layout) {
    return GdxConfig.WORLD_HEIGHT / SIDE_HEIGHT;
  }

  /**
   * The lawn rectangle for this world, as {x, y, width, height} in world pixels with y up.
   *
   * <p>The board is centred across the window and the backdrop is then hung off it, rather than
   * the other way round: the lawn is what the player aims at, so it is the thing that gets to sit
   * in the middle at every window size.
   */
  public static float[] lawnBounds(String seasonKey) {
    return lawnBounds(layoutFor(seasonKey));
  }

  private static float[] lawnBounds(Layout layout) {
    float scale = scaleFor(layout);
    float width = layout.gridWidth() * scale;
    float height = layout.gridHeight() * scale;
    float x = (GdxConfig.WORLD_WIDTH - width) / 2f;
    // gridTop is measured down from the middle strip's own top edge, and that edge is pinned to
    // the top of the window, so this is the same measurement in libGDX's y-up world.
    float y = GdxConfig.WORLD_HEIGHT - (layout.gridTop() + layout.gridHeight()) * scale;
    return new float[] {x, y, width, height};
  }

  /** Left edge of the {@code texture} strip in world pixels, which the side strips hang off. */
  private static float mainLeft(Layout layout) {
    return lawnBounds(layout)[0] - layout.gridX() * scaleFor(layout);
  }

  /**
   * Draws the three strips, in one batch that is already begun.
   *
   * <p>All three are anchored by their top edge, at one scale, at their own heights. A world whose
   * middle strip is taller than its sides therefore hangs a little below the bottom of the window
   * and loses that much of its darkest edge, which is the cheapest of the three ways to reconcile
   * them: stretching the sides puts a growing offset down the seam, and bottom-anchoring puts a
   * hard step there.
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

  /** The atlas each world's backdrop lives in. */
  public static String atlasForSeason(String seasonKey) {
    switch (seasonKey) {
      case "egypt": return "textures/environment/ancientegyptseason.atlas";
      case "frost": return "textures/environment/frostbitecavesseason.atlas";
      case "beach": return "textures/environment/bigwavebeachseason.atlas";
      default: return "textures/environment/darkagesseason.atlas";
    }
  }
}
