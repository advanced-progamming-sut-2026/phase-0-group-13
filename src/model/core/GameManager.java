package model.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.enums.ScoreEvent;
import model.game.Board;
import model.game.MatchResult;
import model.game.ScoreManager;
import model.game.Wave;
import model.game.minigame.SpecialStageRule;
import model.game.plant.Plant;
import model.game.quest.MatchContext;

public class GameManager {

  private Board board;
  private int currentTick;
  private List<Wave> waves;
  private int currentWaveIndex;
  private boolean running;
  private MatchResult matchResult;
  private final ScoreManager scoreManager = new ScoreManager();
  private final Map<String, Integer> lastPlantedTick = new HashMap<>();
  private boolean cooldownsDisabled;
  private boolean freePlanting;
  private boolean bonusMatch;
  private final MatchContext matchContext = new MatchContext();

  private boolean zombieWavesStarted = true;
  private SpecialStageRule activeSpecialRule;
  private model.environment.Season currentSeason;private List<String> eventLogs = new ArrayList<>();

  public void addLog(String message) {
    eventLogs.add(message);
  }

  public List<String> pollLogs() {
    List<String> currentLogs = new ArrayList<>(eventLogs);
    eventLogs.clear();
    return currentLogs;
  }


  public GameManager() {
    this.board = null;
    this.currentTick = 0;
    this.waves = new ArrayList<>();
    this.currentWaveIndex = 0;
    this.running = false;
    this.matchResult = new MatchResult();
  }


  public GameManager(Board board) {
    this() ;
    this.board = board;
  }

  public void initializeLevel(int rows, int cols, List<Wave> levelWaves) {
    this.board = new Board(rows, cols);
    this.waves = new ArrayList<>(levelWaves);
    this.currentWaveIndex = 0;
    this.currentTick = 0;
    this.matchResult = new MatchResult();
  }

  public void startGame() { running = true; }
  public void startZombieWaves() { this.zombieWavesStarted = true; }
  public void pauseZombieWaves() { this.zombieWavesStarted = false; }

  public void setSeason(model.environment.Season season) { this.currentSeason = season; }
  public model.environment.Season getSeason() { return currentSeason; }

  public void setSpecialStageRule(SpecialStageRule rule) { this.activeSpecialRule = rule; }
  public SpecialStageRule getSpecialStageRule() { return activeSpecialRule; }

  public void advanceTime() {
    if (!running || board == null) return;

    currentTick++;
    updateBoardAndSeason();
    processBoardEvents();

    if (checkLoseCondition()) {
      endGame(false);
      return;
    }

    processWaveUpdates();

    if (checkWinCondition()) {
      endGame(true);
    }
  }

  private void updateBoardAndSeason() {
    board.updateAll(currentTick);
    if (currentSeason != null) {
      board.getGameState().update(board.getGameState().getCurrentWave(), currentSeason);
      currentSeason.onTick(board, currentTick);
    }
  }

  private void processBoardEvents() {
    if (zombieWavesStarted && currentWaveIndex < waves.size()) {
      matchContext.onWaveStarted(currentWaveIndex, currentTick);
    }

    for (Board.KillDetail kill : board.drainPendingKillDetails()) {
      matchContext.onZombieKilled(
              currentTick, (int) kill.column(), kill.laneHasUnusedMower(), kill.killedByMower());
    }

    int plantsLostThisTick = board.drainPendingPlantsLostCount();
    for (int i = 0; i < plantsLostThisTick; i++) {
      matchContext.onPlantLost();
    }

    matchContext.refreshFromBoard(board, currentTick);

    for (model.game.reward.Reward reward : board.drainPendingRewards()) {
      matchResult.addEarnedReward(reward);
    }
  }

  private void processWaveUpdates() {
    if (zombieWavesStarted && currentWaveIndex < waves.size()) {
      Wave currentWave = waves.get(currentWaveIndex);
      boolean wasStarted = currentWave.isStarted();
      currentWave.update(board);

      if (!wasStarted && currentWave.isStarted() && currentSeason != null) {
        currentSeason.onWaveStart(board, currentWaveIndex, currentTick);
      }
      if (currentWave.checkCompletion()) {
        currentWaveIndex++;
      }
    }
  }

  private boolean checkLoseCondition() {
    if (activeSpecialRule != null) {
      activeSpecialRule.apply(board.getGameState());
      if (activeSpecialRule.checkLoseCondition(board)) {
        return true;
      }
    }
    return board.isPlayerLost();
  }

  private boolean checkWinCondition() {
    boolean specialEarlyWin = activeSpecialRule != null && activeSpecialRule.checkWinCondition(board);
    if (currentWaveIndex >= waves.size() && board.getZombies().isEmpty()) {
      return true;
    }
    return specialEarlyWin;
  }

  public void endGame(boolean isWin) {
    running = false;
    if (isWin) {
      matchResult.markWin();
      matchResult.calculateRewards();
      System.out.println("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    } else {
      matchResult.markLose();
      System.out.println("The zombie ate your brain; LOSER!!!");
    }
  }

  public boolean placePlant(Plant plant, int row, int col) {
    if (board == null || !running) return false;
    if (activeSpecialRule != null && !activeSpecialRule.isPlantAllowed(plant.getName())) return false;
    if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getColumns()) return false;

    Plant existingPlant = board.getPlantAt(row, col);
    boolean plantIsAquatic = plant.getTags().contains(model.enums.PlantTag.WATER);
    boolean stacking = false;

    if (board.isWaterAt(row, col) && !plantIsAquatic) {
      if (existingPlant == null || !existingPlant.getTags().contains(model.enums.PlantTag.WATER)) {
        return false;
      }
    } else if (existingPlant != null) {
      if (!canStackOn(plant, existingPlant)) return false;
      stacking = true;
    }

    if (!freePlanting) {
      if (!board.getGameState().deductSun(plant.getCost())) return false;
      matchContext.onSunSpent(plant.getCost());
    }

    if (stacking) {
      return stackOnto(plant, existingPlant, row, col);
    }

    board.placePlant(plant);
    matchContext.onPlantPlaced(plant, row, col);
    return true;
  }

  // تگ stack یعنی یا روی هم‌نوع خودش سوار میشه (Pea Pod) یا پوسته‌ی محافظ بقیه‌ست (Pumpkin)
  private boolean canStackOn(Plant plant, Plant existingPlant) {
    if (!plant.getTags().contains(model.enums.PlantTag.STACK)) {
      return false;
    }
    if (plant.getName().equalsIgnoreCase(existingPlant.getName())) {
      return existingPlant.getStackCount() < Plant.MAX_STACK;
    }
    return plant.getCategory() == model.enums.PlantCategory.WALL_NUT
            && existingPlant.getShield() == null;
  }

  private boolean stackOnto(Plant plant, Plant existingPlant, int row, int col) {
    if (plant.getName().equalsIgnoreCase(existingPlant.getName())) {
      existingPlant.addStack();
      System.out.printf("%s now has %d heads stacked at (%d, %d).%n",
              existingPlant.getName(), existingPlant.getStackCount(), col + 1, row + 1);
      return true;
    }
    existingPlant.setShield(plant);
    board.placePlant(plant);
    matchContext.onPlantPlaced(plant, row, col);
    System.out.printf("%s now shields %s at (%d, %d).%n",
            plant.getName(), existingPlant.getName(), col + 1, row + 1);
    return true;
  }

  public Integer collectSunAt(int col, int row) {
    if (board == null || !running) return null;
    Integer amount = board.collectSunAt(col, row);
    if (amount != null && amount > 0) matchContext.onSunCollected(amount);
    return amount;
  }

  public boolean usePlantFood(Plant targetPlant) {
    if (board == null || !running) return false;
    if (board.getGameState().usePlantFood()) {
      targetPlant.applyPlantFood();
      System.out.printf("Plant Food used on %s!%n", targetPlant.getName());
      return true;
    }
    System.out.println("Error: No Plant Food available!");
    return false;
  }

  public int ticksUntilPlantReady(String plantType, int rechargeSeconds) {
    if (cooldownsDisabled || plantType == null) return 0;
    Integer last = lastPlantedTick.get(plantType.toLowerCase());
    if (last == null) return 0;

    int readyAt = last + rechargeSeconds * 10;
    return Math.max(0, readyAt - currentTick);
  }

  public void recordPlanting(String plantType) {
    if (plantType != null) lastPlantedTick.put(plantType.toLowerCase(), currentTick);
  }

  public void disableCooldowns() {
    cooldownsDisabled = true;
    lastPlantedTick.clear();
  }

  public void enableFreePlanting() {
    freePlanting = true;
  }

  public boolean isFreePlanting() {
    return freePlanting;
  }

  public Board getBoard() { return board; }
  public int getSunAmount() { return board != null ? board.getGameState().getCurrentSun() : 0; }
  public int getPlantFoodCount() { return board != null ? board.getGameState().getPlantFoodCount() : 0; }
  public int getCurrentTick() { return currentTick; }
  public boolean isRunning() { return running; }
  public MatchResult getMatchResult() { return matchResult; }
  public ScoreManager getScoreManager() { return scoreManager; }
  public boolean isBonusMatch() { return bonusMatch; }
  public void setBonusMatch(boolean bonusMatch) { this.bonusMatch = bonusMatch; }
  public int getCurrentWaveIndex() { return currentWaveIndex; }
  public int getTotalWaves() { return waves.size(); }
  public int drainPendingKillCount() { return board != null ? board.drainPendingKillCount() : 0; }
  public MatchContext getMatchContext() { return matchContext; }

  public void registerCombatEvent(ScoreEvent event) { registerCombatEvent(event, 1); }
  public void registerCombatEvent(ScoreEvent event, int multiplier) {
    if (!running || event == null) return;
    scoreManager.triggerEvent(event, multiplier);
  }

  public boolean isGameOver() { return !running; }
}