package model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The doc's difficulty rule, level by level and parameter by parameter.
 *
 * <p>It gives one formula for all five effects -- {dl}/3 for anything that goes up, 3/{dl} for
 * anything that goes down -- and says level 3 is assumed throughout. So the two things worth
 * pinning are that every coefficient is exactly 1 at level 3, and that each of the five is on the
 * side of the formula the doc puts it on. The old code used {@code 1 + (dl - 3) * 0.15}, which is
 * neither, and applied it to two of the five.
 *
 * <p>MatchSetup is a process-wide singleton, so each test restores the level it found.
 */
class DifficultyTest {

  private static final double EXACT = 1e-9;

  @AfterEach
  void restoreDefault() {
    MatchSetup.getInstance().setDifficultyLevel(Difficulty.BASELINE_LEVEL);
  }

  private static void at(int level) {
    MatchSetup.getInstance().setDifficultyLevel(level);
  }

  @Test
  void levelThreeIsTheBaselineAndChangesNothing() {
    at(Difficulty.BASELINE_LEVEL);
    assertEquals(1.0, Difficulty.zombieHealth(), EXACT);
    assertEquals(1.0, Difficulty.zombieDamage(), EXACT);
    assertEquals(1.0, Difficulty.zombieSpeed(), EXACT);
    assertEquals(1.0, Difficulty.waveCost(), EXACT);
    assertEquals(1.0, Difficulty.skySunRate(), EXACT);
    assertEquals(1.0, Difficulty.skySunInterval(), EXACT);
  }

  @Test
  void theIncreaseCoefficientIsTheLevelOverThree() {
    for (int level = Difficulty.MIN_LEVEL; level <= Difficulty.MAX_LEVEL; level++) {
      assertEquals(level / 3.0, Difficulty.increase(level), EXACT, "level " + level);
    }
  }

  @Test
  void theDecreaseCoefficientIsThreeOverTheLevel() {
    for (int level = Difficulty.MIN_LEVEL; level <= Difficulty.MAX_LEVEL; level++) {
      assertEquals(3.0 / level, Difficulty.decrease(level), EXACT, "level " + level);
    }
  }

  @Test
  void everyLevelScalesEveryParameterTheWayTheDocSays() {
    for (int level = Difficulty.MIN_LEVEL; level <= Difficulty.MAX_LEVEL; level++) {
      at(level);
      String where = "at level " + level;
      // the three that go up
      assertEquals(level / 3.0, Difficulty.zombieHealth(), EXACT, where + ", zombie health");
      assertEquals(level / 3.0, Difficulty.zombieDamage(), EXACT, where + ", zombie damage");
      assertEquals(level / 3.0, Difficulty.zombieSpeed(), EXACT, where + ", advance speed");
      // the two that go down
      assertEquals(3.0 / level, Difficulty.waveCost(), EXACT, where + ", wave cost");
      assertEquals(3.0 / level, Difficulty.skySunRate(), EXACT, where + ", sky sun rate");
      // and the interval, which is the reciprocal of the rate
      assertEquals(level / 3.0, Difficulty.skySunInterval(), EXACT, where + ", sun interval");
    }
  }

  @Test
  void harderMeansToughersFasterZombiesAndLessSun() {
    at(Difficulty.MIN_LEVEL);
    double easyHealth = Difficulty.zombieHealth();
    double easySpeed = Difficulty.zombieSpeed();
    double easySunRate = Difficulty.skySunRate();
    double easyCost = Difficulty.waveCost();

    at(Difficulty.MAX_LEVEL);
    assertTrue(Difficulty.zombieHealth() > easyHealth, "zombies should be tougher at level 5");
    assertTrue(Difficulty.zombieSpeed() > easySpeed, "zombies should advance faster at level 5");
    assertTrue(Difficulty.skySunRate() < easySunRate, "less sun should fall at level 5");
    assertTrue(Difficulty.waveCost() < easyCost, "zombies should cost less at level 5");
  }

  @Test
  void aLevelOutsideOneToFiveIsClampedRatherThanTakenLiterally() {
    assertEquals(Difficulty.MIN_LEVEL, Difficulty.clamp(0));
    assertEquals(Difficulty.MIN_LEVEL, Difficulty.clamp(-7));
    assertEquals(Difficulty.MAX_LEVEL, Difficulty.clamp(9));

    at(0);
    assertEquals(Difficulty.MIN_LEVEL / 3.0, Difficulty.zombieHealth(), EXACT);
    at(99);
    assertEquals(Difficulty.MAX_LEVEL / 3.0, Difficulty.zombieHealth(), EXACT);
    // a clamped level can never divide by zero
    at(0);
    assertTrue(Double.isFinite(Difficulty.skySunInterval()));
  }

  @Test
  void noCoefficientEverReachesZeroOrInfinity() {
    for (int level = Difficulty.MIN_LEVEL; level <= Difficulty.MAX_LEVEL; level++) {
      at(level);
      for (double coefficient : new double[] {Difficulty.zombieHealth(), Difficulty.zombieDamage(),
          Difficulty.zombieSpeed(), Difficulty.waveCost(), Difficulty.skySunRate(),
          Difficulty.skySunInterval()}) {
        assertTrue(coefficient > 0 && Double.isFinite(coefficient),
            "level " + level + " produced " + coefficient);
      }
    }
  }
}
