package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The doc gives Zomboss "three health segments" with a stun after each one is depleted, so the
 * only thing this type has to get right is where the boundaries between those three fall and how
 * full each one is at a given health.
 */
class ZombossHealthTest {

  /** Egypt's own numbers out of Zombies.json. */
  private static final List<Integer> EGYPT_STAGES = List.of(4000, 8000, 6500);
  private static final int EGYPT_TOTAL = 18500;

  @Test
  void thereAreAlwaysExactlyThreeSegments() {
    assertEquals(3, ZombossHealth.SEGMENTS);
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);
    assertEquals(3, health.segmentsLeft(EGYPT_TOTAL));
  }

  @Test
  void theSegmentsAddUpToTheHealthTheBossActuallyHas() {
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);
    int sum = 0;
    for (int i = 0; i < ZombossHealth.SEGMENTS; i++) {
      sum += health.capacityOf(i);
    }
    assertEquals(EGYPT_TOTAL, sum);
  }

  @Test
  void theSheetsOwnStageSizesAreKept() {
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);
    assertEquals(4000, health.capacityOf(0));
    assertEquals(8000, health.capacityOf(1));
    assertEquals(6500, health.capacityOf(2));
  }

  @Test
  void difficultyScalingRescalesTheSegmentsInsteadOfBreakingThem() {
    // Hard mode gives the same boss more health; the split has to follow it.
    int scaled = EGYPT_TOTAL * 2;
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, scaled);
    int sum = 0;
    for (int i = 0; i < ZombossHealth.SEGMENTS; i++) {
      sum += health.capacityOf(i);
    }
    assertEquals(scaled, sum, "the segments must still add up to the real total");
    assertEquals(8000, health.capacityOf(0), "and keep the sheet's proportions");
  }

  @Test
  void aBossWithNoStagesOnItsSheetStillGetsThreeSegments() {
    ZombossHealth health = new ZombossHealth(List.of(), 9000);
    assertEquals(3000, health.capacityOf(0));
    assertEquals(3000, health.capacityOf(1));
    assertEquals(3000, health.capacityOf(2));
  }

  @Test
  void segmentsEmptyOneAtATimeInOrder() {
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);

    assertEquals(0, health.segmentsCleared(EGYPT_TOTAL), "nothing is gone at full health");

    // Exactly the first segment's worth of damage.
    assertEquals(1, health.segmentsCleared(EGYPT_TOTAL - 4000));
    assertEquals(0f, health.fractionOf(0, EGYPT_TOTAL - 4000));
    assertEquals(1f, health.fractionOf(1, EGYPT_TOTAL - 4000));

    // Through the second one as well.
    assertEquals(2, health.segmentsCleared(EGYPT_TOTAL - 12000));
    assertEquals(1f, health.fractionOf(2, EGYPT_TOTAL - 12000));

    assertEquals(3, health.segmentsCleared(0), "a dead boss has nothing left");
  }

  @Test
  void aPartlyEatenSegmentReportsHowMuchOfItIsLeft() {
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);
    int afterHalfTheFirst = EGYPT_TOTAL - 2000;
    assertEquals(0.5f, health.fractionOf(0, afterHalfTheFirst), 0.001f);
    assertEquals(1f, health.fractionOf(1, afterHalfTheFirst), "later segments are untouched");
    assertEquals(0, health.segmentsCleared(afterHalfTheFirst));
  }

  @Test
  void everyFractionStaysBetweenZeroAndOneAcrossTheWholeFight() {
    ZombossHealth health = new ZombossHealth(EGYPT_STAGES, EGYPT_TOTAL);
    for (int hp = EGYPT_TOTAL; hp >= 0; hp -= 250) {
      for (int segment = 0; segment < ZombossHealth.SEGMENTS; segment++) {
        float fraction = health.fractionOf(segment, hp);
        assertTrue(fraction >= 0f && fraction <= 1f,
            "segment " + segment + " at " + hp + " hp reported " + fraction);
      }
    }
  }
}
