package view.gdx.render;

import view.gdx.core.GdxConfig;

/**
 * When a shooter's attack clip plays, relative to the tick its shot is created on.
 *
 * <p>A shot does not leave at the end of the shooting animation. It leaves at the frame the plant
 * is furthest into the throw, and the rest of the clip is the plant recovering from it. Measured
 * off the rigs by tracking how far forward the muzzle reaches across the clip: a Peashooter peaks
 * 62% of the way in, a Repeater 63%, a Mega Gatling 62%, a Split Pea 69%, a Pea Pod 68%, a
 * Starfruit 57%. The pults throw earlier and spend longer recovering -- Cabbage 38%, Kernel 40%,
 * Melon 34%, Pepper 28%.
 *
 * <p>The two numbers below are where a sweep of the whole roster puts the muzzle furthest into
 * the throw, scored against each rig's own range of movement: 0.64 for the shooters and 0.40 for
 * the pults. Playing the whole clip before the shot, as this used to, released at 1.0, which is
 * barely a quarter of the way out -- the plant had finished the throw and settled back before the
 * pea appeared.
 */
final class AttackSchedule {

  /** Where the shot leaves a shooter's attack clip. */
  static final float SHOOTER_RELEASE = 0.64f;
  /** Where it leaves a pult's, which lets go earlier and takes longer to come back down. */
  static final float LOB_RELEASE = 0.4f;

  private AttackSchedule() {}

  /**
   * Where in the clip to draw this frame, in seconds, or -1 for a plant that is not shooting.
   *
   * <p>The release frame lands on the tick the shot is created: the clip runs at its authored
   * speed up to that frame across the ticks before it, and carries on from it across the ticks
   * after. The wind-up is cut short rather than started early when the firing interval is tighter
   * than the clip.
   *
   * @param now the current tick plus how far through that tick this frame is
   */
  static float clipTime(float length, float release, int lastActionTick, int interval, float now) {
    if (length <= 0f || interval <= 0) {
      return -1f;
    }
    float held = Math.max(0f, Math.min(1f, release)) * length;
    float since = (now - lastActionTick) * GdxConfig.SECONDS_PER_TICK;
    if (lastActionTick > 0 && since >= 0f && since < length - held) {
      return held + since;
    }
    float until = (lastActionTick + interval - now) * GdxConfig.SECONDS_PER_TICK;
    float lead = Math.min(interval * GdxConfig.SECONDS_PER_TICK, held);
    return until > 0f && until <= lead ? Math.max(0f, held - until) : -1f;
  }
}
