package model.account;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Result;

public class Progress {

  /** فاز ۱: مینی‌گیم‌ها از همان اول از travel log در دسترسند، قفل پیشرفت ندارند. */
  public static final List<String> ALL_MINI_GAMES =
      Arrays.asList("vasebreaker", "wallnut_bowling", "i_zombie", "beghouled", "zombotany");

  private int currentStage;
  private int currentLevel;

  private boolean miniGamesUnlocked;
  private boolean survivalModeUnlocked;

  private Set<String> unlockedMiniGames;

  /** بالاترین مرحله‌ای که از هر مینی‌گیم پاس شده (اسم مینی‌گیم → مرحله). */
  private Map<String, Integer> clearedMiniGameLevels;

  private int maxClearedStage;
  private int maxClearedLevel;

  /**
   * True once {@code MAX_STAGES}-{@code LEVELS_PER_STAGE} (4-4) has been cleared. The map has no
   * stage 5 to move on to, so instead of walking past the end the cursor stays on the last level
   * and this flag records that the adventure is finished.
   */
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

  /** سیوهای قدیمی با قفل مینی‌گیم ذخیره شده‌اند؛ Gson سازنده را صدا نمی‌زند، اینجا بازشان می‌کنیم. */
  private void openMiniGames() {
    if (unlockedMiniGames == null) {
      unlockedMiniGames = new HashSet<>();
    }
    miniGamesUnlocked = true;
    unlockedMiniGames.addAll(ALL_MINI_GAMES);
  }

  public static final int MINI_GAME_LEVELS = 3;

  /** بعد از برد در یک مینی‌گیم، بالاترین مرحلهٔ پاس‌شده را ثبت می‌کند. */
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

  /** بالاترین مرحلهٔ پاس‌شدهٔ یک مینی‌گیم (۰ یعنی هنوز هیچ مرحله‌ای پاس نشده). */
  public int getClearedMiniGameLevel(String miniGameName) {
    if (clearedMiniGameLevels == null || miniGameName == null) {
      return 0;
    }
    return clearedMiniGameLevels.getOrDefault(miniGameName.toLowerCase().trim(), 0);
  }

  /** مجموع مراحل پاس‌شدهٔ همهٔ مینی‌گیم‌ها (برای لیدربورد). */
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
      // 4-4 is the last level on the map. There is no stage 5 to walk into, so the cursor stays
      // where it is and the adventure is marked finished instead.
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

    // Once the map is finished every level stays open for replay.
    if (this.adventureCompleted) return true;

    if (stage < this.currentStage) return true;

    if (stage == this.currentStage) {
      return level <= this.currentLevel;
    }

    return false;
  }

  /** Debug cheat: opens every chapter and level so the whole adventure can be reached. */
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

  /** True once 4-4 has been cleared: the map is finished and there is nothing left to unlock. */
  public boolean isAdventureCompleted() {
    normalize();
    return adventureCompleted;
  }

  /**
   * Pulls the cursor back inside the 4x4 map.
   *
   * <p>Gson builds this object field-by-field without running the constructor, so a save written
   * by an older build -- when the map was 5 stages of 10 levels, and when clearing the last level
   * still incremented the stage -- can arrive holding a stage or level that no longer exists.
   * Rather than let those values reach the menus, they are clamped to the last real level and the
   * adventure is treated as finished, which is what walking off the end of the map meant.
   */
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
