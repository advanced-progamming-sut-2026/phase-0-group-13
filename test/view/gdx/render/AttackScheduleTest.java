package view.gdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Where a shooter's attack clip is at, frame by frame, around the tick its shot is created.
 *
 * <p>The thing that has to be exactly right is one moment: the frame the plant is furthest into
 * its throw must be the frame the projectile appears. Everything else -- the wind-up before it and
 * the recovery after -- only has to run at the clip's own speed and stay inside the clip.
 */
class AttackScheduleTest {

  private static final float CLIP = 1.0333f;
  private static final int INTERVAL = 15;
  private static final float RELEASE = AttackSchedule.SHOOTER_RELEASE;
  private static final float TOLERANCE = 1e-4f;

  private static float at(float now, int lastActionTick) {
    return AttackSchedule.clipTime(CLIP, RELEASE, lastActionTick, INTERVAL, now);
  }

  @Test
  void theShotLandsOnTheFrameThePlantIsFurthestIntoItsThrow() {
    // The tick the projectile is created on: the model has just set lastActionTick to it.
    assertEquals(RELEASE * CLIP, at(15f, 15), TOLERANCE,
        "the shot must appear on the release frame, not at the end of the animation");
  }

  @Test
  void theWindUpArrivesAtTheReleaseFrameExactlyAsTheTickTurns() {
    // The last frame before the shot, drawn against the tick the plant has not fired on yet.
    float justBefore = at(15f - 1e-4f, 0);
    assertEquals(RELEASE * CLIP, justBefore, 1e-3f,
        "the wind-up ended somewhere other than the frame the shot leaves on");
  }

  @Test
  void theWindUpRunsForwardsAtTheClipsOwnSpeed() {
    float previous = -1f;
    for (int step = 1; step <= 60; step++) {
      float now = 15f - step / 4f;
      float time = at(now, 0);
      if (time < 0f) {
        continue;
      }
      assertTrue(time <= RELEASE * CLIP + TOLERANCE, "the wind-up overran the release frame");
      if (previous >= 0f) {
        assertEquals(0.025f, previous - time, 1e-3f,
            "a quarter tick of frame time should be a quarter tick of clip time");
      }
      previous = time;
    }
    assertTrue(previous >= 0f, "the plant never wound up at all");
  }

  @Test
  void theRestOfTheClipPlaysOutAfterTheShotAndThenStops() {
    float last = RELEASE * CLIP;
    for (int step = 1; step <= 40; step++) {
      float now = 15f + step / 4f;
      float time = at(now, 15);
      if (time < 0f) {
        // Once the recovery is over the plant is back to standing about, until the next wind-up.
        assertTrue(last >= CLIP - 0.1f,
            "the plant dropped its attack at " + last + "s of a " + CLIP + "s clip");
        return;
      }
      assertTrue(time > last, "the recovery should carry on forwards from the shot");
      assertTrue(time <= CLIP + TOLERANCE, "the recovery ran off the end of the clip");
      last = time;
    }
    throw new AssertionError("the attack clip never ended");
  }

  @Test
  void aPlantStandsStillForTheRestOfItsInterval() {
    // A Peashooter's clip is a second and its interval a second and a half, so there is half a
    // second in the middle where it is doing nothing at all and must not be miming.
    assertEquals(-1f, at(15f + INTERVAL / 2f, 15), TOLERANCE,
        "the plant should be idle between the recovery and the next wind-up");
  }

  @Test
  void aPultLetsGoEarlierInItsSwing() {
    float cabbage = 1.6667f;
    assertEquals(AttackSchedule.LOB_RELEASE * cabbage,
        AttackSchedule.clipTime(cabbage, AttackSchedule.LOB_RELEASE, 30, 40, 30f), TOLERANCE,
        "a pult's shot leaves partway through the swing, not at the end of it");
    assertTrue(AttackSchedule.LOB_RELEASE < AttackSchedule.SHOOTER_RELEASE,
        "a pult throws earlier in its clip than a shooter does");
  }

  @Test
  void aClipLongerThanTheIntervalIsCutShortRatherThanStartedEarly() {
    // Winter Melon's clip is 2.2s against a much tighter interval; the wind-up has to give way,
    // and what it must not do is start before the previous shot.
    float length = 2.1667f;
    int interval = 10;
    float time = AttackSchedule.clipTime(length, RELEASE, 20, interval, 20.5f);
    assertTrue(time >= 0f, "the plant should still be animating its shot");
    assertTrue(time <= length, "the clip cannot be drawn past its own end");
    assertEquals(-1f, AttackSchedule.clipTime(length, RELEASE, 20, interval, 19.5f), TOLERANCE,
        "the wind-up reached back past the shot before it");
  }

  @Test
  void aBehaviourWithNoIntervalIsNotScheduledAtAll() {
    assertEquals(-1f, AttackSchedule.clipTime(CLIP, RELEASE, 15, 0, 16f), TOLERANCE,
        "with no cadence there is nothing to schedule against, and the old hold takes over");
  }
}
