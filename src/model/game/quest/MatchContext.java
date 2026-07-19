package model.game.quest;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import model.enums.PlantTag;
import model.game.Board;
import model.game.plant.Plant;


public final class MatchContext {

  public static final int TICKS_PER_SECOND = 10;
  private static final int OPENING_WINDOW_TICKS = 30 * TICKS_PER_SECOND;

  private int sunSpent;
  private int sunCollected;

  private int totalZombiesKilled;
  private int zombiesKilledInOpeningWindow;
  private int zombiesKilledAtColumnZeroWithNoMower;

  private int plantsPlacedCount;
  private int explosivePlantsPlaced;
  private int sunProducerPlantsPlaced;
  private int plantsLost;
  private final Set<PlantTag> plantFamiliesPlaced = EnumSet.noneOf(PlantTag.class);
  private final Set<Integer> plantedColumns = new HashSet<>();
  private final Set<Integer> plantedRows = new HashSet<>();

  private int firstWaveStartTick = -1;
  private int currentTick;

  private boolean gardenSymmetric;
  private boolean gardenSymmetricExceptMiddleRow;
  private int boardRows;
  private int boardColumns;


  private int winStreakAtMaxDifficulty;
  private boolean matchWon;

  public void setWinStreakAtMaxDifficulty(int streak) {
    this.winStreakAtMaxDifficulty = streak;
  }

  public int getWinStreakAtMaxDifficulty() {
    return winStreakAtMaxDifficulty;
  }

  public void setMatchWon(boolean matchWon) {
    this.matchWon = matchWon;
  }

  public boolean isMatchWon() {
    return matchWon;
  }



  public void onSunSpent(int amount) {
    if (amount > 0) sunSpent += amount;
  }

  public void onSunCollected(int amount) {
    if (amount > 0) sunCollected += amount;
  }

  public void onPlantPlaced(Plant plant, int row, int col) {
    if (plant == null) return;
    plantsPlacedCount++;
    plantedRows.add(row);
    plantedColumns.add(col);
    plantFamiliesPlaced.addAll(plant.getTags());
    if (plant.getTags().contains(PlantTag.EXPLOSIVE)) explosivePlantsPlaced++;
    if (plant.getTags().contains(PlantTag.SUN)) sunProducerPlantsPlaced++;
  }

  public void onPlantLost() {
    plantsLost++;
  }

  public void onWaveStarted(int waveIndex, int tick) {
    if (waveIndex == 0 && firstWaveStartTick < 0) {
      firstWaveStartTick = tick;
    }
  }


  public void onZombieKilled(int tick, int column, boolean laneHasUnusedMower) {
    totalZombiesKilled++;
    if (firstWaveStartTick >= 0 && tick - firstWaveStartTick <= OPENING_WINDOW_TICKS) {
      zombiesKilledInOpeningWindow++;
    }
    if (column == 0 && !laneHasUnusedMower) {
      zombiesKilledAtColumnZeroWithNoMower++;
    }
  }

  public void refreshFromBoard(Board board, int tick) {
    if (board == null) return;
    this.currentTick = tick;
    this.boardRows = board.getRows();
    this.boardColumns = board.getColumns();
    recomputeSymmetry(board);
  }

  private void recomputeSymmetry(Board board) {
    int rows = board.getRows();
    int cols = board.getColumns();
    boolean symmetric = true;
    boolean symmetricExceptMiddle = true;
    int middleRow = rows / 2;

    for (int row = 0; row < rows; row++) {
      int mirrorRow = rows - 1 - row;
      for (int col = 0; col < cols; col++) {
        boolean occupied = board.getPlantAt(row, col) != null;
        boolean mirrorOccupied = board.getPlantAt(mirrorRow, col) != null;
        if (occupied != mirrorOccupied) {
          symmetric = false;
          if (row != middleRow && mirrorRow != middleRow) {
            symmetricExceptMiddle = false;
          }
        }
      }
    }
    this.gardenSymmetric = symmetric;
    this.gardenSymmetricExceptMiddleRow = symmetricExceptMiddle;
  }


  public int getSunSpent() { return sunSpent; }
  public int getSunCollected() { return sunCollected; }
  public int getTotalZombiesKilled() { return totalZombiesKilled; }
  public int getZombiesKilledInOpeningWindow() { return zombiesKilledInOpeningWindow; }
  public int getZombiesKilledAtColumnZeroWithNoMower() { return zombiesKilledAtColumnZeroWithNoMower; }
  public int getPlantsPlacedCount() { return plantsPlacedCount; }
  public int getExplosivePlantsPlaced() { return explosivePlantsPlaced; }
  public int getSunProducerPlantsPlaced() { return sunProducerPlantsPlaced; }
  public int getPlantsLost() { return plantsLost; }
  public Set<PlantTag> getPlantFamiliesPlaced() { return plantFamiliesPlaced; }
  public boolean isGardenSymmetric() { return gardenSymmetric; }
  public boolean isGardenSymmetricExceptMiddleRow() { return gardenSymmetricExceptMiddleRow; }
  public int getCurrentTick() { return currentTick; }

  public boolean isColumnEmpty(int columnZeroBased) {
    return !plantedColumns.contains(columnZeroBased);
  }

  public boolean isRowEmpty(int rowZeroBased) {
    return !plantedRows.contains(rowZeroBased);
  }

  public boolean onlyPlacedFamily(PlantTag family) {
    return !plantFamiliesPlaced.isEmpty()
        && plantFamiliesPlaced.size() == 1
        && plantFamiliesPlaced.contains(family);
  }

  public boolean neverPlacedFamily(PlantTag family) {
    return !plantFamiliesPlaced.contains(family);
  }
}