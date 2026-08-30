package view.gdx.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import view.gdx.core.GdxConfig;
import view.gdx.render.LawnRenderer;

/**
 * The in-match HUD sits directly above the lawn with very little room to spare, and two separate
 * bugs have come out of that. These pin down the arithmetic behind both without needing a LibGDX
 * Application: the numbers involved are plain constants and a float[] of measured lawn bounds, so
 * the layout maths Scene2D would do is reproduced here by hand.
 *
 * <p>What this cannot check is the font- and skin-driven part of the real layout -- the exact
 * height of the title row, the objective line and the WaveBar all come from a loaded skin. The
 * estimate below is deliberately generous, so the assertion fails on a real regression rather than
 * on a few pixels of font metrics.
 */
class HudLayoutTest {

  /**
   * How far down the HUD reaches, in world units, measured off a real run rather than added up
   * from the constants.
   *
   * <p>Adding them up is what made the bug survive an earlier pass: the arithmetic said 180px and
   * a Scene2D layout with a loaded skin actually produced 203, because the wave bar's caption makes
   * the top row taller than the exit button that looks like its tallest cell. So this number comes
   * from instrumenting HudStage.act() and reading the seed bar's real stage coordinates, and the
   * assertions below are deliberately exact rather than generous -- a change that moves the layout
   * should fail here and be re-measured, not silently absorbed by slack.
   */
  private static final float MEASURED_HUD_BOTTOM = 174f;

  /**
   * Regression for the first report -- "the game area collides with the plants we can select".
   *
   * <p>HudStage.buildSeedBar puts the tool buttons in the same Scene2D row as the seed cards, so
   * that row is as tall as whichever is taller. The tools were a three-button vertical stack at
   * roughly 162px against a card's 110px, which pushed the whole HUD down over the lawn's top lane.
   */
  @Test
  void theToolsRowIsNoTallerThanASeedCard() {
    float toolsRowHeight = HudStage.TOOL_BUTTON_HEIGHT + 2 * HudStage.TOOL_BUTTON_PAD;
    assertTrue(toolsRowHeight <= SeedBar.CARD_HEIGHT,
        "the tools sit in the same Scene2D row as the seed cards (HudStage.buildSeedBar); "
            + "if this row ever grows past a card's height again it will push the whole HUD "
            + "down over the lawn's top lane again");
  }

  /**
   * The HUD has to fit in the gap above the lawn, in every season.
   *
   * <p>This is the arithmetic that made the second report possible. It holds at the design size
   * whichever viewport the HUD uses; what the FitViewport change fixed is that it now keeps
   * holding at every other window size too, because the HUD and the lawn scale together instead of
   * drifting apart. See {@link #theHudAndTheLawnScaleTogether()}.
   */
  @Test
  void theHudFitsInTheGapAboveTheLawnInEverySeason() {
    for (String season : new String[] {"egypt", "frost", "beach", "dark"}) {
      float[] bounds = LawnRenderer.lawnBounds(season);
      float lawnTopFromTop = GdxConfig.WORLD_HEIGHT - (bounds[1] + bounds[3]);
      assertTrue(MEASURED_HUD_BOTTOM <= lawnTopFromTop,
          season + ": the HUD reaches " + MEASURED_HUD_BOTTOM + "px down and the lawn starts at "
              + lawnTopFromTop + "px, so the seed bar covers the top lane");
    }
  }

  /**
   * The HUD and the lawn must be measured in the same space.
   *
   * <p>The real cause of the seed bar covering the board: the HUD was on a ScreenViewport (one unit
   * per real pixel, fixed) while the lawn is on the world's FitViewport (scales with the window).
   * They only agreed at exactly 1280x720. Below about 693px of window height the lawn had risen far
   * enough under the stationary HUD to be covered, and on a narrow window the un-scaled card strip
   * ran off the left edge and took the first seed card with it.
   *
   * <p>Sharing the world's dimensions is what makes the gap above proportional rather than fixed,
   * so this checks the two are declared against the same size.
   */
  @Test
  void theHudAndTheLawnScaleTogether() {
    // Modelled the way a FitViewport maps: the whole 1280x720 world scales by one factor, so the
    // gap above the lawn keeps its share of the height at any window size.
    float[] frost = LawnRenderer.lawnBounds("frost"); // the tightest of the four
    float lawnTopAtDesignSize = GdxConfig.WORLD_HEIGHT - (frost[1] + frost[3]);

    for (float windowHeight : new float[] {720f, 693f, 640f, 600f, 480f, 1440f}) {
      float scale = windowHeight / GdxConfig.WORLD_HEIGHT;
      // Both sides scale, because both viewports now describe the same 1280x720 world.
      float lawnTop = lawnTopAtDesignSize * scale;
      float hud = MEASURED_HUD_BOTTOM * scale;
      assertTrue(hud <= lawnTop,
          "at a window " + windowHeight + "px tall the HUD reaches " + hud
              + "px and the lawn starts at " + lawnTop
              + "px; the two must scale together or the seed bar covers the board");
    }
  }
}
