package model.environment;

import java.util.ArrayList;
import java.util.List;
import model.enums.PlantTag;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class BigWaveBeachSeason extends Season {
  private static final int TIDE_DESTROY_DAMAGE = 10000;

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
    return zombiesByAliasKeyword("beach");
  }

  @Override
  public List<Tile> generateMap() {
    // خونه‌های آب (برای Lily Pad و گیاهان شناور) هنوز تو Tile مدل نشدن؛ فعلا شبکه معمولی برمیگردونیم
    return plainGrid();
  }
  @Override
  public void onWaveStart(Board board, int waveNumber, int currentTick) {
    tideHigh = !tideHigh;
    System.out.printf(
            "The tide is %s for wave %d!%n", tideHigh ? "rising" : "receding", waveNumber + 1);

    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        board.setWaterAt(row, col, tideHigh);
      }
    }

    if (!tideHigh) {
      return;
    }

    for (Plant plant : new ArrayList<>(board.getPlants())) {
      boolean aquatic = plant.getTags().contains(PlantTag.WATER);
      if (!aquatic && !plant.isDead() && !isProtectedByLilyPad(board, plant)) {
        plant.takeDamage(TIDE_DESTROY_DAMAGE);
        System.out.printf("The rising tide swept away %s!%n", plant.getName());
      }
    }
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