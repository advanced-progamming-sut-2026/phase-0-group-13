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
  /** تعداد ستون‌های سمت راست که در جزر (کم‌ترین حالت) دریا هستند. */
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

  @Override
  public List<Zombie> getAvailableZombies() {
    // Big Wave Beach: Fisherman, Snorkel, Octopus
    return rosterOf(ZombieType.FISHERMAN, ZombieType.SNORKEL, ZombieType.OCTOPUS);
  }

  @Override
  public String getBossZombieName() {
    return "ZombieZombossMechPirate";
  }

  @Override
  public List<Tile> generateMap() {
    // خونه‌های آب (برای Lily Pad و گیاهان شناور) هنوز تو Tile مدل نشدن؛ فعلا شبکه معمولی برمیگردونیم
    return plainGrid();
  }
  /** در شروع مرحله، دریا با جزرِ پایه (۳ ستون سمت راست) روی نقشه نشانده می‌شود. */
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

    // فقط گیاهانی که تازه زیر آب رفته‌اند و شناور نیستند از بین می‌روند (نه کل گیاهان زمین)
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

  /** ستون‌های سمت راست را آب و بقیه را خشکی می‌کند و شمارهٔ اولین ستون آب را برمی‌گرداند. */
  private int applyWaterLine(Board board, int waterColumns) {
    int firstWaterColumn = Math.max(1, board.getColumns() - waterColumns);
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