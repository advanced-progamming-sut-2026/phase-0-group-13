package controller.MainMenuSubControllers.GameMenuSubControllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import network.protocol.Payloads;
import org.junit.jupiter.api.Test;

/**
 * The terminal leaderboard reads Payloads.LeaderboardEntry rows the server built, not local User
 * objects, so the columns it used to compute itself are now derived from that record instead.
 * These cover the three derivations that are not a straight field read.
 *
 * <p>No server is started: the rows are the same record the wire carries, so the mapping can be
 * checked directly. That the rows come from the server at all is enforced by the fact that this
 * class has no way to build one from a local User.
 */
class ScoreBoardMenuControllerTest {

  private static Payloads.LeaderboardEntry row(String name, int season, int stage, int miniGames,
      int daily, int other, Integer myPoint) {
    return new Payloads.LeaderboardEntry(name, season, stage, miniGames, daily, other, myPoint);
  }

  @Test
  void questsColumnAddsDailyAndOther() {
    assertEquals(7, ScoreBoardMenuController.completedQuests(row("a", 1, 1, 0, 5, 2, null)));
    assertEquals(0, ScoreBoardMenuController.completedQuests(row("a", 1, 1, 0, 0, 0, null)));
  }

  @Test
  void progressShowsSeasonAndStageAndDashesAnAccountThatClearedNothing() {
    assertEquals("2-3", ScoreBoardMenuController.progressOf(row("a", 2, 3, 0, 0, 0, null)));
    assertEquals("-", ScoreBoardMenuController.progressOf(row("a", 0, 0, 0, 0, 0, null)));
    assertEquals("-", ScoreBoardMenuController.progressOf(row("a", 1, 0, 0, 0, 0, null)));
  }

  @Test
  void aPlayerWhoNeverFinishedABonusRunRanksBelowOneWhoScoredZero() {
    Payloads.LeaderboardEntry unplayed = row("unplayed", 1, 1, 0, 0, 0, null);
    Payloads.LeaderboardEntry zero = row("zero", 1, 1, 0, 0, 0, 0);
    Payloads.LeaderboardEntry high = row("high", 1, 1, 0, 0, 0, 500);

    List<Payloads.LeaderboardEntry> ranked = new ArrayList<>(List.of(unplayed, high, zero));
    // the same comparator the High Score command builds, descending as the menu shows it first
    ranked.sort(Comparator.comparingInt(ScoreBoardMenuController::meowPoints).reversed()
        .thenComparing(Payloads.LeaderboardEntry::username));

    assertEquals(List.of("high", "zero", "unplayed"),
        ranked.stream().map(Payloads.LeaderboardEntry::username).toList());
    assertTrue(ScoreBoardMenuController.meowPoints(unplayed)
        < ScoreBoardMenuController.meowPoints(zero));
  }
}
