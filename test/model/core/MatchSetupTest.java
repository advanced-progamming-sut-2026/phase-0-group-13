package model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.enums.MiniGameType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * MatchSetup is a singleton the whole graphical build shares, so every test resets it afterwards
 * rather than trusting test order.
 *
 * <p>Covers the bonusRun flag added this session: {@link MatchLauncher#selectionRule()} has to see
 * it, or the Bonus Game's deck screen would apply a random adventure stage's plant lock to a run
 * that belongs to no stage at all.
 */
class MatchSetupTest {

  @AfterEach
  void resetSingleton() {
    MatchSetup.reset();
  }

  @Test
  void settingABonusRunClearsAnyChapterOrMiniGame() {
    MatchSetup setup = MatchSetup.getInstance();
    setup.setTargetChapter("2");
    setup.setMiniGame(MiniGameType.VASEBREAKER, 3);

    setup.setBonusRun();

    assertTrue(setup.isBonusRun());
    assertEquals(null, setup.getTargetChapter());
    assertEquals(MiniGameType.NONE, setup.getCurrentMiniGame());
  }

  @Test
  void enteringAChapterClearsAPreviousBonusRun() {
    MatchSetup setup = MatchSetup.getInstance();
    setup.setBonusRun();

    setup.setTargetChapter("1");

    assertFalse(setup.isBonusRun(), "a chapter entry must not leave a stale bonus flag behind");
  }

  @Test
  void startingAMiniGameClearsAPreviousBonusRun() {
    MatchSetup setup = MatchSetup.getInstance();
    setup.setBonusRun();

    setup.setMiniGame(MiniGameType.WALLNUT_BOWLING, 1);

    assertFalse(setup.isBonusRun());
  }

  /**
   * Before this session's fix, selectionRule() only checked getCurrentMiniGame(), so a bonus run
   * launched right after a special-stage attempt could inherit that stage's plant lock. The check
   * is now the first thing selectionRule() does, so it never even reads which chapter was left
   * behind.
   */
  @Test
  void aBonusRunAlwaysGetsAnUnrestrictedSelectionRuleRegardlessOfTheChapterLeftBehind() {
    MatchSetup setup = MatchSetup.getInstance();
    setup.setTargetChapter("1");
    setup.setBonusRun();

    assertEquals(null, MatchLauncher.selectionRule(),
        "the bonus run belongs to no stage, so nothing may lock its plants");
  }
}
