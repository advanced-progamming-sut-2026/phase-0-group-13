package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class AncientEgyptSeason extends Season {
  public AncientEgyptSeason() {
    this.name = "Ancient Egypt";
  }

  private static final int TORNADO_MIN_COLUMNS = 1;
  private static final int TORNADO_MAX_COLUMNS = 4;

  private final java.util.Random random = new java.util.Random();

  @Override
  public void placeHazards(model.game.Board board) {
    board.placeRandomTombstones(3, 5, 700);
  }

  /**
   * گردباد: فقط در موج آخر، زامبی‌ها به جای ورود عادی از لبه، بین ۱ تا ۴ ستون جلوتر وارد
   * می‌شوند.
   */
  @Override
  public void onWaveStart(
          model.game.Board board, int waveNumber, int currentTick, boolean finalWave) {
    if (!finalWave) {
      return;
    }
    int moved = 0;
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      int jump = TORNADO_MIN_COLUMNS + random.nextInt(TORNADO_MAX_COLUMNS - TORNADO_MIN_COLUMNS + 1);
      zombie.setX(Math.max(0, zombie.getX() - jump));
      moved++;
    }
    if (moved > 0) {
      System.out.printf("A sandstorm blows %d zombie(s) deep into the lawn!%n", moved);
    }
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return zombiesByAliasKeyword("egypt", "mummy", "ra", "tombraiser");
  }

  @Override
  public String getBossZombieName() {
    return "ZombieZombossMechEgypt";
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }
}