package model.account;

import java.util.HashSet;
import java.util.Set;
import model.Result;

public class Progress {
  private int currentStage;
  private int currentLevel;

  private boolean miniGamesUnlocked;
  private boolean survivalModeUnlocked;

  private Set<String> unlockedMiniGames;

  private int maxClearedStage;
  private int maxClearedLevel;

  public Progress() {
    this.currentStage = 1;
    this.currentLevel = 1;
    this.maxClearedStage = 0;
    this.maxClearedLevel = 0;
    this.miniGamesUnlocked = false;
    this.survivalModeUnlocked = false;
    this.unlockedMiniGames = new HashSet<>();
  }

  public Result advanceAdventure() {
    if (this.currentStage > this.maxClearedStage
        || (this.currentStage == this.maxClearedStage
            && this.currentLevel > this.maxClearedLevel)) {
      this.maxClearedStage = this.currentStage;
      this.maxClearedLevel = this.currentLevel;
    }

    int previousStage = this.currentStage;
    int previousLevel = this.currentLevel;

    if (this.currentLevel < 4) {
      this.currentLevel++;
    } else {
      this.currentStage++;
      this.currentLevel = 1;
    }

    String unlockMessage = "";
    if (this.currentStage == 2 && this.currentLevel == 1 && !miniGamesUnlocked) {
      this.miniGamesUnlocked = true;
      unlockMessage = "  Mini-Games mode has been unlocked!";
    } else if (this.currentStage == 3 && this.currentLevel == 1 && !survivalModeUnlocked) {
      this.survivalModeUnlocked = true;
      unlockMessage = "  Survival mode has been unlocked!";
    }

    String msg =
        String.format(
            "Adventure progressed from %d-%d to %d-%d.%s",
            previousStage, previousLevel, this.currentStage, this.currentLevel, unlockMessage);

    return new Result(true, msg, this);
  }

  public Result unlockMiniGame(String miniGameName) {
    if (!miniGamesUnlocked) {
      return new Result(false, "Mini-Games mode is still locked for this account!", null);
    }

    String gameKey = miniGameName.toLowerCase().trim();
    if (unlockedMiniGames.contains(gameKey)) {
      return new Result(false, miniGameName + " is already unlocked.", null);
    }

    unlockedMiniGames.add(gameKey);
    return new Result(true, "Successfully unlocked mini-game: " + miniGameName, gameKey);
  }

  public boolean isLevelAccessible(int stage, int level) {
    if (stage < 1 || level < 1 || level > 10) return false;

    if (stage < this.currentStage) return true;

    if (stage == this.currentStage) {
      return level <= this.currentLevel;
    }

    return false;
  }

  public boolean isMiniGameUnlocked(String miniGameName) {
    return miniGamesUnlocked && unlockedMiniGames.contains(miniGameName.toLowerCase().trim());
  }

  public int getCurrentStage() {
    return currentStage;
  }

  public int getCurrentLevel() {
    return currentLevel;
  }

  public boolean isMiniGamesUnlocked() {
    return miniGamesUnlocked;
  }

  public boolean isSurvivalModeUnlocked() {
    return survivalModeUnlocked;
  }

  public Set<String> getUnlockedMiniGames() {
    return unlockedMiniGames;
  }

  public int getMaxClearedStage() {
    return maxClearedStage;
  }

  public int getMaxClearedLevel() {
    return maxClearedLevel;
  }

  public void setAdventureProgress(int stage, int level) {
    if (stage > 0 && level > 0 && level <= 10) {
      this.currentStage = stage;
      this.currentLevel = level;
    }
  }
}
