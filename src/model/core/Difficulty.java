package model.core;

/**
 * The difficulty coefficients, in one place.
 *
 * <p>The doc names five things the difficulty level moves, and gives one rule for all of them:
 * "for an increase use the coefficient {dl}/3, and for a decrease 3/{dl}", with 3 assumed
 * everywhere unless stated otherwise. So level 3 is the baseline and every coefficient is exactly
 * 1 there, which is also the level a new account is created at.
 *
 * <table>
 *   <caption>What the doc says moves, and which way</caption>
 *   <tr><th>Zombie health</th><td>up</td><td>{@link #zombieHealth()}</td></tr>
 *   <tr><th>Zombie damage</th><td>up</td><td>{@link #zombieDamage()}</td></tr>
 *   <tr><th>The game's rate of advance</th><td>up</td><td>{@link #zombieSpeed()}</td></tr>
 *   <tr><th>The wave cost of zombies</th><td>down</td><td>{@link #waveCost()}</td></tr>
 *   <tr><th>Rate of sun falling from the sky</th><td>down</td><td>{@link #skySunRate()}</td></tr>
 * </table>
 *
 * <p>Every system reads the level from the same place, {@link MatchSetup}, which the menus fill in
 * from the account when a match is set up. Before this, WaveGenerator and ZombieFactory each
 * carried their own {@code 1 + (dl - 3) * 0.15} -- a curve that is not the doc's, applied to two
 * of the five things and to none of the other three.
 */
public final class Difficulty {

  public static final int MIN_LEVEL = 1;
  public static final int MAX_LEVEL = 5;

  /** The doc's assumed level. Every coefficient is 1 here. */
  public static final int BASELINE_LEVEL = 3;

  private Difficulty() {
  }

  /** The level the current match is being played at, clamped to the range the doc allows. */
  public static int level() {
    return clamp(MatchSetup.getInstance().getDifficultyLevel());
  }

  public static int clamp(int level) {
    return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
  }

  /** {dl}/3, the doc's coefficient for anything that goes up with difficulty. */
  public static double increase(int level) {
    return clamp(level) / (double) BASELINE_LEVEL;
  }

  /** 3/{dl}, the doc's coefficient for anything that goes down with difficulty. */
  public static double decrease(int level) {
    return BASELINE_LEVEL / (double) clamp(level);
  }

  public static double zombieHealth() {
    return increase(level());
  }

  public static double zombieDamage() {
    return increase(level());
  }

  /** How much faster zombies come at you; the doc's "rate of advance". */
  public static double zombieSpeed() {
    return increase(level());
  }

  /** What a zombie costs against a wave's budget: less, so more of them fit in one wave. */
  public static double waveCost() {
    return decrease(level());
  }

  /** How often sun falls. */
  public static double skySunRate() {
    return decrease(level());
  }

  /**
   * The gap between two sky suns, which is the reciprocal of the rate.
   *
   * <p>SunManager is written in intervals rather than rates, and inverting the coefficient at the
   * call site is exactly the kind of thing that gets inverted the wrong way round once.
   */
  public static double skySunInterval() {
    return 1.0 / skySunRate();
  }
}
