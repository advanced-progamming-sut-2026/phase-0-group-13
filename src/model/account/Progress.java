package model.account;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Result;

public class Progress {

  public static final List<String> ALL_MINI_GAMES =
      Arrays.asList("vasebreaker", "wallnut_bowling", "i_zombie", "beghouled", "zombotany");

  private int currentStage;
  private int currentLevel;

  private boolean miniGamesUnlocked;
  private boolean survivalModeUnlocked;

  private Set<String> unlockedMiniGames;

  private Map<String, Integer> clearedMiniGameLevels;

  private int maxClearedStage;
  private int maxClearedLevel;

  private boolean adventureCompleted;

  public Progress() {
    this.currentStage = 1;
    this.currentLevel = 1;
    this.maxClearedStage = 0;
    this.maxClearedLevel = 0;
    this.adventureCompleted = false;
    this.miniGamesUnlocked = true;
    this.survivalModeUnlocked = false;
    this.unlockedMiniGames = new HashSet<>(ALL_MINI_GAMES);
    this.clearedMiniGameLevels = new HashMap<>();
  }

  private void openMiniGames() {
    if (unlockedMiniGames == null) {
      unlockedMiniGames = new HashSet<>();
    }
    miniGamesUnlocked = true;
    unlockedMiniGames.addAll(ALL_MINI_GAMES);
  }

  public static final int MINI_GAME_LEVELS = 3;

  public boolean recordMiniGameCleared(String miniGameName, int level) {
    if (miniGameName == null || level < 1) {
      return false;
    }
    if (clearedMiniGameLevels == null) {
      clearedMiniGameLevels = new HashMap<>();
    }
    String key = miniGameName.toLowerCase().trim();
    int best = clearedMiniGameLevels.getOrDefault(key, 0);
    if (level <= best) {
      return false;
    }
    clearedMiniGameLevels.put(key, level);
    return true;
  }

  public int getClearedMiniGameLevel(String miniGameName) {
    if (clearedMiniGameLevels == null || miniGameName == null) {
      return 0;
    }
    return clearedMiniGameLevels.getOrDefault(miniGameName.toLowerCase().trim(), 0);
  }

  public int getTotalMiniGameLevelsCleared() {
    if (clearedMiniGameLevels == null) {
      return 0;
    }
    int total = 0;
    for (int level : clearedMiniGameLevels.values()) {
      total += level;
    }
    return total;
  }

  public Result advanceAdventure() {
    normalize();
    if (this.adventureCompleted) {
      return new Result(
          true,
          String.format(
              "The adventure is already complete -- %d-%d was the final level.",
              AdventureMap.MAX_STAGES, AdventureMap.LEVELS_PER_STAGE),
          this);
    }

    if (this.currentStage > this.maxClearedStage
        || (this.currentStage == this.maxClearedStage
            && this.currentLevel > this.maxClearedLevel)) {
      this.maxClearedStage = this.currentStage;
      this.maxClearedLevel = this.currentLevel;
    }

    int previousStage = this.currentStage;
    int previousLevel = this.currentLevel;

    if (this.currentLevel < AdventureMap.LEVELS_PER_STAGE) {
      this.currentLevel++;
    } else if (this.currentStage < AdventureMap.MAX_STAGES) {
      this.currentStage++;
      this.currentLevel = 1;
    } else {
      this.adventureCompleted = true;
      return new Result(
          true,
          String.format(
              "Adventure complete! %d-%d was the final level -- every chapter is cleared.",
              previousStage, previousLevel),
          this);
    }

    String unlockMessage = "";
    if (this.currentStage == 3 && this.currentLevel == 1 && !survivalModeUnlocked) {
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
    openMiniGames();
    String gameKey = miniGameName.toLowerCase().trim();
    if (unlockedMiniGames.contains(gameKey)) {
      return new Result(false, miniGameName + " is already unlocked.", null);
    }

    unlockedMiniGames.add(gameKey);
    return new Result(true, "Successfully unlocked mini-game: " + miniGameName, gameKey);
  }

  public boolean isLevelAccessible(int stage, int level) {
    if (stage < 1
        || stage > AdventureMap.MAX_STAGES
        || level < 1
        || level > AdventureMap.LEVELS_PER_STAGE) {
      return false;
    }
    normalize();

    if (this.adventureCompleted) return true;

    if (stage < this.currentStage) return true;

    if (stage == this.currentStage) {
      return level <= this.currentLevel;
    }

    return false;
  }

  public Result unlockAllChapters() {
    this.currentStage = AdventureMap.MAX_STAGES;
    this.currentLevel = AdventureMap.LEVELS_PER_STAGE;
    this.maxClearedStage = AdventureMap.MAX_STAGES;
    this.maxClearedLevel = AdventureMap.LEVELS_PER_STAGE;
    this.adventureCompleted = true;
    this.survivalModeUnlocked = true;
    openMiniGames();
    return new Result(true, "All chapters and levels unlocked.", null);
  }

  public boolean isMiniGameUnlocked(String miniGameName) {
    openMiniGames();
    return unlockedMiniGames.contains(miniGameName.toLowerCase().trim());
  }

  public int getCurrentStage() {
    normalize();
    return currentStage;
  }

  public int getCurrentLevel() {
    normalize();
    return currentLevel;
  }

  public boolean isAdventureCompleted() {
    normalize();
    return adventureCompleted;
  }

  private void normalize() {
    if (currentStage < 1) {
      currentStage = 1;
    }
    if (currentLevel < 1) {
      currentLevel = 1;
    }
    if (currentStage > AdventureMap.MAX_STAGES) {
      currentStage = AdventureMap.MAX_STAGES;
      currentLevel = AdventureMap.LEVELS_PER_STAGE;
      adventureCompleted = true;
    }
    if (currentLevel > AdventureMap.LEVELS_PER_STAGE) {
      currentLevel = AdventureMap.LEVELS_PER_STAGE;
    }
    if (maxClearedStage > AdventureMap.MAX_STAGES) {
      maxClearedStage = AdventureMap.MAX_STAGES;
      maxClearedLevel = AdventureMap.LEVELS_PER_STAGE;
    }
    if (maxClearedLevel > AdventureMap.LEVELS_PER_STAGE) {
      maxClearedLevel = AdventureMap.LEVELS_PER_STAGE;
    }
  }

  public boolean isMiniGamesUnlocked() {
    openMiniGames();
    return miniGamesUnlocked;
  }

  public boolean isSurvivalModeUnlocked() {
    return survivalModeUnlocked;
  }

  public Set<String> getUnlockedMiniGames() {
    openMiniGames();
    return unlockedMiniGames;
  }

  public int getMaxClearedStage() {
    return maxClearedStage;
  }

  public int getMaxClearedLevel() {
    return maxClearedLevel;
  }

  /**
   * Moves the cursor to a level that exists on the map. Anything outside the 4x4 grid is refused
   * rather than clamped, so a bad call cannot silently send the player somewhere else.
   */
  public void setAdventureProgress(int stage, int level) {
    if (stage >= 1
        && stage <= AdventureMap.MAX_STAGES
        && level >= 1
        && level <= AdventureMap.LEVELS_PER_STAGE) {
      this.currentStage = stage;
      this.currentLevel = level;
      this.adventureCompleted = false;
    }
  }
}
