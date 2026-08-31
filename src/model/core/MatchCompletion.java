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

public final class MatchCompletion {

  private MatchCompletion() {}

  public static void apply(GameManager match) {
    if (match == null || match.getMatchResult() == null) {
      return;
    }
    MatchResult result = match.getMatchResult();
    if (result.isWon() && allLawnmowersUnused(match)) {
      match.registerCombatEvent(ScoreEvent.WAVE_CLEARED_NO_LOSS);
    }
    int bonusScore = match.getScoreManager().getCurrentMatchScore();
    if (match.isBonusMatch()) {
      System.out.println("Game Bonus finished! MyoPoints earned this run: " + bonusScore);
    }

    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }
    if (match.isBonusMatch()) {
      match.getScoreManager().applyScoresToUser(user);
      submitBonusScore(bonusScore);
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
