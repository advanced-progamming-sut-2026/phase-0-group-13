package model.environment;

import java.util.ArrayList;
import java.util.List;
import model.enums.PlantTag;
import model.game.Board;
import model.game.GameState;
import model.enums.ZombieType;
import model.game.Tile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class BigWaveBeachSeason extends Season {
  private static final int TIDE_DESTROY_DAMAGE = 10000;
  // طبق داک فقط چند ستون سمت راست دریاست و سطح آب بین جزر و مد تغییر می‌کند (مثلا ۳ ستون تا ۵ ستون)
  private static final int LOW_TIDE_COLUMNS = 3;
  /** خط قرمز مد: آب حداکثر تا این تعداد ستونِ سمت راست بالا می‌آید. */
  private static final int HIGH_TIDE_COLUMNS = 5;

  private boolean tideHigh = false;

  public BigWaveBeachSeason() {
    this.name = "Big Wave Beach";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  /**
   * The sea roster only.
   *
   * <p>{@link Season#rosterOf} puts the fifteen common land zombies in front of whatever a season
   * asks for, and zombies enter at the right-hand column -- which on this map is open sea at every
   * tide. So the beach was spawning Coneheads and All-Stars into the water and walking them
   * across it. Only the zombies that can be on water belong in this pool.
   */
  @Override
  public List<Zombie> getAvailableZombies() {
    List<Zombie> pool = new ArrayList<>();
    for (Zombie zombie : rosterOf(ZombieType.FISHERMAN, ZombieType.SNORKEL, ZombieType.OCTOPUS)) {
      if (zombie.canCrossWater()) {
        pool.add(zombie);
      }
    }
    return pool;
  }

  @Override
  public String getBossZombieName() {
    return "ZombieZombossMechPirate";
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }
  @Override
  public void placeHazards(Board board) {
    applyWaterLine(board, LOW_TIDE_COLUMNS);
  }

  @Override
  public void onWaveStart(Board board, int waveNumber, int currentTick) {
    tideHigh = !tideHigh;
    int firstWaterColumn = applyWaterLine(board, tideHigh ? HIGH_TIDE_COLUMNS : LOW_TIDE_COLUMNS);
    System.out.printf(
            "The tide is %s for wave %d; the sea now starts at column %d.%n",
            tideHigh ? "rising" : "receding", waveNumber + 1, firstWaterColumn + 1);

    if (!tideHigh) {
      return;
    }

    for (Plant plant : new ArrayList<>(board.getPlants())) {
      if (plant.isDead() || !board.isWaterAt(plant.getRow(), plant.getCol())) {
        continue;
      }
      boolean aquatic = plant.getTags().contains(PlantTag.WATER);
      if (!aquatic && !isProtectedByLilyPad(board, plant)) {
        plant.takeDamage(TIDE_DESTROY_DAMAGE);
        System.out.printf("The rising tide swept away %s!%n", plant.getName());
      }
    }
  }

  public int getMaxWaterColumn(int boardColumns) {
    return firstWaterColumn(boardColumns, HIGH_TIDE_COLUMNS);
  }

  public int getLowTideColumn(int boardColumns) {
    return firstWaterColumn(boardColumns, LOW_TIDE_COLUMNS);
  }

  public boolean isTideHigh() {
    return tideHigh;
  }

  private static int firstWaterColumn(int boardColumns, int waterColumns) {
    return Math.max(1, boardColumns - waterColumns);
  }

  private int applyWaterLine(Board board, int waterColumns) {
    int firstWaterColumn = firstWaterColumn(board.getColumns(), waterColumns);
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        board.setWaterAt(row, col, col >= firstWaterColumn);
      }
    }
    return firstWaterColumn;
  }

  private boolean isProtectedByLilyPad(Board board, Plant plant) {
    for (Plant other : board.getPlants()) {
      if (other != plant
              && !other.isDead()
              && other.getRow() == plant.getRow()
              && other.getCol() == plant.getCol()
              && other.getTags().contains(PlantTag.WATER)) {
        return true;
      }
    }
    return false;
  }
}
