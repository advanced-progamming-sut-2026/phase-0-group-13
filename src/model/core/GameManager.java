package model.core;

import java.util.ArrayList;
import java.util.List;
import model.account.AdventureMap;
import model.game.Board;
import model.game.MatchResult;
import model.game.ScoreManager;
import model.game.Sun;
import model.game.Wave;
import model.game.news.AllNews;
import model.game.plant.Plant;
import model.game.quest.Quest;

public class GameManager {

  private Board board;
  private int currentTick;
  private List<Wave> waves;
  private int currentWaveIndex;
  private boolean running;
  private MatchResult matchResult;
  private final ScoreManager scoreManager = new ScoreManager();
  private AllNews allnews;
  private List<Quest> quests;
  private AdventureMap adventureMap;

  public GameManager() {
    this.board = null;
    this.currentTick = 0;
    this.waves = new ArrayList<>();
    this.currentWaveIndex = 0;
    this.running = false;
    this.matchResult = new MatchResult();
    this.allnews = new AllNews(null);
  }

  public GameManager(Board board) {
    this.board = board;
    this.currentTick = 0;
    this.waves = new ArrayList<>();
    this.currentWaveIndex = 0;
    this.running = false;
    this.matchResult = new MatchResult();
  }

  public void initializeLevel(int rows, int cols, List<Wave> levelWaves) {
    this.board = new Board(rows, cols);
    this.waves = new ArrayList<>(levelWaves);
    this.currentWaveIndex = 0;
    this.currentTick = 0;
    this.matchResult = new MatchResult();
  }

  public void startGame() {
    running = true;
  }

  public void advanceTime() {
    if (!running || board == null) {
      return;
    }

    currentTick++;
    board.updateAll(currentTick);

    if (board.isPlayerLost()) {
      endGame();
      matchResult.markLose();
      return;
    }

    if (currentWaveIndex < waves.size()) {
      Wave currentWave = waves.get(currentWaveIndex);
      currentWave.update(board);

      if (currentWave.checkCompletion()) {
        currentWaveIndex++;
      }
    }

    if (checkWinCondition()) {
      endGame();
      matchResult.markWin();
      matchResult.calculateRewards();
    }
  }

  public boolean placePlant(Plant plant, int row, int col) {
    if (board == null || !running) {
      return false;
    }

    if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getColumns()) {
      return false;
    }

    if (board.getPlantAt(row, col) != null) {
      return false;
    }

    if (!board.getGameState().deductSun(plant.getCost())) {
      return false;
    }

    board.placePlant(plant);
    return true;
  }

  public void collectSun(Sun sun) {
    if (board != null && running) {
      sun.collect(board.getGameState());
    }
  }

  public boolean usePlantFood(Plant targetPlant) {
    if (board == null || !running) {
      return false;
    }

    if (board.getGameState().usePlantFood()) {
      targetPlant.applyPlantFood();
      System.out.printf("Plant Food used on %s!%n", targetPlant.getName());
      return true;
    }

    System.out.println("Error: No Plant Food available!");
    return false;
  }

  private boolean checkWinCondition() {
    if (currentWaveIndex >= waves.size()) {
      return board.getZombies().isEmpty();
    }
    return false;
  }

  public boolean isGameOver() {
    return !running;
  }

  public void endGame() {
    running = false;
    System.out.println("Game ended.");
  }

  public Board getBoard() {
    return board;
  }

  public int getSunAmount() {
    return board != null ? board.getGameState().getCurrentSun() : 0;
  }

  public int getPlantFoodCount() {
    return board != null ? board.getGameState().getPlantFoodCount() : 0;
  }

  public int getCurrentTick() {
    return currentTick;
  }

  public boolean isRunning() {
    return running;
  }

  public MatchResult getMatchResult() {
    return matchResult;
  }

  public ScoreManager getScoreManager() {
    return scoreManager;
  }

  public int getCurrentWaveIndex() {
    return currentWaveIndex;
  }

  public int getTotalWaves() {
    return waves.size();
  }
}
