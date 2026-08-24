package model.core;

import java.util.ArrayList;
import java.util.List;
import model.enums.MiniGameType;

public class MatchSetup {
  private static MatchSetup instance;

  private String targetChapter;
  private int targetLevel;
  private List<String> selectedPlants;
  private List<String> boostedPlants;

  private MiniGameType currentMiniGame;
  private int miniGameLevel;
  private int difficultyLevel = 3;
  private int maxDeckSlots = 8;

  private MatchSetup() {
    selectedPlants = new ArrayList<>();
    boostedPlants = new ArrayList<>();
    currentMiniGame = MiniGameType.NONE;
    miniGameLevel = 1;
  }

  public static MatchSetup getInstance() {
    if (instance == null) {
      instance = new MatchSetup();
    }
    return instance;
  }

  public static void reset() {
    instance = null;
  }

  public void setTargetChapter(String targetChapter) {
    this.targetChapter = targetChapter;
    this.targetLevel = 0;
    this.currentMiniGame = MiniGameType.NONE;
  }

  public String getTargetChapter() {
    return targetChapter;
  }

  /** Which level of the chapter the player picked; 0 means "wherever the account is up to". */
  public void setTargetLevel(int targetLevel) {
    this.targetLevel = Math.max(0, targetLevel);
  }

  public int getTargetLevel() {
    return targetLevel;
  }

  public void setMiniGame(MiniGameType type, int level) {
    this.currentMiniGame = type;
    this.miniGameLevel = level;
    this.targetChapter = null;
    this.targetLevel = 0;
  }

  public MiniGameType getCurrentMiniGame() {
    return currentMiniGame;
  }

  public int getMiniGameLevel() {
    return miniGameLevel;
  }

  public void setSelectedPlants(List<String> plants) {
    this.selectedPlants = plants;
  }

  public List<String> getSelectedPlants() {
    return selectedPlants;
  }

  public void setBoostedPlants(List<String> plants) {
    this.boostedPlants = plants;
  }

  public List<String> getBoostedPlants() {
    return boostedPlants;
  }

  public void setDifficultyLevel(int difficultyLevel) {
    this.difficultyLevel = difficultyLevel;
  }

  public int getDifficultyLevel() {
    return difficultyLevel;
  }

  public int getMaxDeckSlots() {
    return maxDeckSlots;
  }

  public void setMaxDeckSlots(int maxDeckSlots) {
    this.maxDeckSlots = maxDeckSlots;
  }

  public int getMinDeckSlots() {
    return 2;
  }
}