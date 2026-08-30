package model.account;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The debug unlock has to open both gates. Chapter rows on the Adventure screen read
 * {@link User#getUnlockedStages()} while the level grid reads
 * {@link Progress#isLevelAccessible(int, int)}, so opening only one of them leaves every chapter
 * past the first still showing LOCKED.
 */
class UnlockAllChaptersTest {

  @Test
  void everyChapterAndLevelOpens() {
    User user = new User("u", "h", "u@example.com", "n", "male");
    user.unlockAllChapters();

    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      assertTrue(user.getUnlockedStages().contains("stage_" + stage),
          "chapter " + stage + " is still locked on the map");
      for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
        assertTrue(user.getProgress().isLevelAccessible(stage, level),
            "level " + stage + "-" + level + " is still locked");
      }
    }
  }

  @Test
  void aFreshAccountOnlyHasTheFirstChapter() {
    User user = new User("u", "h", "u@example.com", "n", "male");
    assertTrue(user.getUnlockedStages().contains("stage_1"));
    for (int stage = 2; stage <= AdventureMap.MAX_STAGES; stage++) {
      assertTrue(!user.getUnlockedStages().contains("stage_" + stage),
          "chapter " + stage + " should start locked");
    }
  }
}
