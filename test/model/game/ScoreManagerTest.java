package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import model.account.User;
import model.enums.ScoreEvent;
import org.junit.jupiter.api.Test;

/**
 * Locks in the exact behaviour {@code model.core.MatchCompletion} depends on: a bonus run's score
 * has to be read <em>before</em> {@link ScoreManager#applyScoresToUser}, because that call clears
 * it. A regression that reads it after would silently submit zero to the server on every run --
 * which is exactly the bug that shipped until this session, since nothing failed loudly, the
 * server just never saw a non-zero score. See MatchCompletion.apply()'s "Read before
 * applyScoresToUser" comment.
 */
class ScoreManagerTest {

  private static User freshUser() {
    return new User("scoretest", "hash", "score@example.com", "Score Tester", "male");
  }

  @Test
  void triggerEventAccumulatesPointsWithTheMultiplier() {
    ScoreManager score = new ScoreManager();
    score.triggerEvent(ScoreEvent.KILL_FAST_ZOMBIE);
    score.triggerEvent(ScoreEvent.KILL_FAST_ZOMBIE, 2);
    int expected = ScoreEvent.KILL_FAST_ZOMBIE.getPoints() * 3;
    assertEquals(expected, score.getCurrentMatchScore());
  }

  @Test
  void applyScoresToUserBanksThePointsOnTheAccount() {
    ScoreManager score = new ScoreManager();
    score.triggerEvent(ScoreEvent.KILL_FAST_ZOMBIE);
    User user = freshUser();
    int before = user.getMeowPoints();

    score.applyScoresToUser(user);

    assertEquals(before + ScoreEvent.KILL_FAST_ZOMBIE.getPoints(), user.getMeowPoints());
  }

  /**
   * The fact that made the bug possible: once banked, the match score is gone. A caller that
   * wants to know what a run scored has to ask before this, not after.
   */
  @Test
  void applyScoresToUserResetsTheRunCounterToZero() {
    ScoreManager score = new ScoreManager();
    score.triggerEvent(ScoreEvent.KILL_FAST_ZOMBIE);
    assertEquals(ScoreEvent.KILL_FAST_ZOMBIE.getPoints(), score.getCurrentMatchScore(),
        "sanity check: there is something to bank");

    score.applyScoresToUser(freshUser());

    assertEquals(0, score.getCurrentMatchScore(),
        "banking the score must clear the run counter, which is why MatchCompletion and "
            + "GameplayScreen both capture it into a local variable first");
  }

  @Test
  void applyScoresToUserWithAZeroScoreDoesNotTouchTheAccount() {
    ScoreManager score = new ScoreManager();
    User user = freshUser();
    int before = user.getMeowPoints();

    score.applyScoresToUser(user);

    assertEquals(before, user.getMeowPoints());
  }

  @Test
  void applyScoresToUserWithANullUserDoesNotThrow() {
    ScoreManager score = new ScoreManager();
    score.triggerEvent(ScoreEvent.KILL_FAST_ZOMBIE);
    assertDoesNotThrow(() -> score.applyScoresToUser(null));
  }
}
