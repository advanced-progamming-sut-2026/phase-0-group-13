package model.core;

import data.GameDataManager;
import data.persistence.UserManager;
import java.io.IOException;
import model.account.AdventureMap;
import model.account.Progress;
import model.account.User;
import model.enums.MiniGameType;
import model.enums.ScoreEvent;
import model.game.Lawnmower;
import model.game.MatchResult;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.reward.Reward;
import network.client.ClientSession;
import network.protocol.Payloads;

/**
 * Everything that happens to the account once a match is over: score, quests, level reward and
 * adventure progression.
 *
 * <p>It lives here rather than in a controller because both builds finish a level: the typed
 * GamePlayController when the last tick runs out, and the graphical GameplayScreen when the match
 * stops running. It used to be private to the typed one, so the graphical build showed "Level
 * cleared!" and then unlocked nothing.
 */
public final class MatchCompletion {

  private MatchCompletion() {}

  /** Call once, when {@code match.isRunning()} has just gone false. */
  public static void apply(GameManager match) {
    if (match == null || match.getMatchResult() == null) {
      return;
    }
    MatchResult result = match.getMatchResult();
    if (result.isWon() && allLawnmowersUnused(match)) {
      match.registerCombatEvent(ScoreEvent.WAVE_CLEARED_NO_LOSS);
    }
    if (match.isBonusMatch()) {
      System.out.println("Game Bonus finished! MyoPoints earned this run: "
              + match.getScoreManager().getCurrentMatchScore());
    }

    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }
    match.getScoreManager().applyScoresToUser(user);
    if (match.isBonusMatch()) {
      submitBonusScore(match.getScoreManager().getCurrentMatchScore());
      save();
      return;
    }

    user.addMatchResult(result);
    user.updateDifficultyWinStreak(result.isWon());
    match.getMatchContext().setMatchWon(result.isWon());
    user.evaluateContextualQuests(match.getMatchContext());

    for (Reward earned : result.getEarnedRewards()) {
      earned.apply(user);
    }

    MiniGameType miniGame = MatchSetup.getInstance().getCurrentMiniGame();
    if (miniGame != MiniGameType.NONE) {
      // مینی‌گیم پیشرفت ادونچر رو جلو نمیبره؛ فقط مرحله‌ی خودش ثبت میشه
      if (result.isWon()) {
        int level = MatchSetup.getInstance().getMiniGameLevel();
        System.out.println("You cleared " + miniGame + " (Level " + level + ")!");
        MiniGameLauncher.awardClear(user, miniGame, level);
      }
      save();
      return;
    }

    if (result.isWon()) {
      advance(user, MatchLauncher.stageNumber(), MatchLauncher.levelInStage());
    }
    save();
  }

  /**
   * Only the level the player is actually up to moves the map on; replaying a cleared one is
   * allowed but hands out neither the reward nor the next unlock a second time.
   */
  private static void advance(User user, int stage, int level) {
    Progress progress = user.getProgress();
    user.triggerQuestEvent("STAGE_CLEAR", 1);
    if (progress.isAdventureCompleted()
            || stage != progress.getCurrentStage()
            || level != progress.getCurrentLevel()) {
      System.out.printf("Replayed level %d-%d; adventure progress is already past it.%n",
              stage, level);
      return;
    }
    grantLevelReward(user, AdventureMap.getLevelReward(stage, level));
    System.out.println(progress.advanceAdventure().message());
    // Clearing the last level does not move the cursor on, so there is no new chapter to unlock;
    // unlocking blindly here is what used to create a "stage_5" that the map has no room for.
    if (!progress.isAdventureCompleted()) {
      user.unlockItem("stage_" + progress.getCurrentStage());
    }
  }

  private static boolean allLawnmowersUnused(GameManager match) {
    if (match.getBoard() == null) {
      return false;
    }
    for (Lawnmower lawnmower : match.getBoard().getLawnmowers()) {
      if (!lawnmower.isActive()) {
        return false;
      }
    }
    return true;
  }

  /**
   * جایزه‌ی مرحله را می‌دهد. اگر جایزه‌ی از پیش تعیین‌شده گیاهی باشد که بازیکن همین الان دارد،
   * به جایش اولین گیاه قفل‌بودهٔ بازی داده می‌شود تا جایزه همیشه چیز جدیدی باشد.
   */
  private static void grantLevelReward(User user, model.Result reward) {
    String unlockId = reward.success() && reward.getObject() instanceof String id ? id : null;

    if (unlockId != null && unlockId.contains("trophy")) {
      System.out.println(reward.message());
      return;
    }

    if (unlockId == null || user.hasUnlockedPlant(unlockId)) {
      unlockId = firstLockedPlant(user);
    }

    if (unlockId == null) {
      System.out.println("You already own every plant in the game!");
      return;
    }

    model.Result unlocked = user.unlockPlant(unlockId);
    System.out.println("Reward Unlocked: " + unlockId + "!");
    if (!unlocked.success()) {
      System.out.println(unlocked.message());
    }
  }

  /** اولین گیاهی که بازیکن هنوز باز نکرده (ترتیب فایل plants.json = ترتیب پیشرفت). */
  private static String firstLockedPlant(User user) {
    if (GameDataManager.plantRepository == null) {
      return null;
    }
    for (PlantTemplate template : GameDataManager.plantRepository.getAll()) {
      if (template.name != null && !user.hasUnlockedPlant(template.name)) {
        return template.name.toLowerCase();
      }
    }
    return null;
  }

  /**
   * Sends a bonus run's My-Point to the server, which is the only thing that ever fills the
   * leaderboard's My Point column.
   *
   * <p>The server keeps the higher of the two, so sending a worse run is harmless and the record
   * is decided there rather than here. A failure is reported and swallowed: the run itself already
   * counted towards the account, and losing the connection should not lose the match.
   */
  private static void submitBonusScore(int score) {
    ClientSession session = ClientSession.getInstance();
    if (!session.isAuthenticated()) {
      return;
    }
    try {
      Payloads.ScoreResponse response = session.submitScore(score);
      System.out.println(response.improved()
              ? "New bonus-game record on the leaderboard: " + response.bestScore()
              : "Bonus run scored " + score + "; your record is still " + response.bestScore());
    } catch (IOException e) {
      System.out.println("could not send the bonus score to the server: " + e.getMessage());
    }
  }

  private static void save() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
