package model.core;

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

  public static double waveCost() {
    return decrease(level());
  }

  public static double skySunRate() {
    return decrease(level());
  }

  public static double skySunInterval() {
    return 1.0 / skySunRate();
  }
}
