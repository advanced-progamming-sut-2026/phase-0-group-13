package view.gdx.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression for a real overlap: HudStage.buildSeedBar puts the tool buttons in the same Scene2D
 * row as the seed bar's cards (see its "Seeds and tools share a row" comment), so that row's
 * height becomes whichever of the two is taller. The tools used to be a three-button vertical
 * stack -- Shovel, Food, Pause -- at roughly 162px, well past a card's 110px, which pushed the
 * whole HUD down far enough to cover the lawn's top lane. The report was "the game area collides
 * with the plants we can select."
 *
 * <p>No LibGDX Application is needed for this: the two class-level constants involved
 * (HudStage.TOOL_BUTTON_HEIGHT / _PAD and SeedBar.CARD_HEIGHT) are plain floats, so the row-height
 * arithmetic Scene2D's Table would do is reproduced by hand here. It cannot re-derive font or
 * skin-driven parts of the real layout (the title row, the objective line, WaveBar), only this one
 * specific relationship -- but that is also the one piece the fix actually changed.
 */
class HudToolsRowHeightTest {

  @Test
  void theToolsRowIsNoTallerThanASeedCard() {
    float toolsRowHeight = HudStage.TOOL_BUTTON_HEIGHT + 2 * HudStage.TOOL_BUTTON_PAD;
    assertTrue(toolsRowHeight <= SeedBar.CARD_HEIGHT,
        "the tools sit in the same Scene2D row as the seed cards (HudStage.buildSeedBar); "
            + "if this row ever grows past a card's height again, it will push the whole HUD "
            + "down over the lawn's top lane, same as before this fix");
  }
}
