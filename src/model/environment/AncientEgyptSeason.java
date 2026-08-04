package model.environment;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class AncientEgyptSeason extends Season {
  // داک: «فقط در موج آخر» ممکن است زامبی به‌جای ورود عادی، با گردباد و بین ۱ تا ۴ ستون جلوتر از
  // نقطهٔ ورود عادی وارد نقشه شود
  private static final double TORNADO_CHANCE = 0.5;
  private static final int MIN_TORNADO_JUMP = 1;
  private static final int MAX_TORNADO_JUMP = 4;

  private final Random random = new Random();
  private final Set<Zombie> tornadoChecked =
          Collections.newSetFromMap(new IdentityHashMap<Zombie, Boolean>());

  public AncientEgyptSeason() {
    this.name = "Ancient Egypt";
  }

  private static final int TORNADO_MIN_COLUMNS = 1;
  private static final int TORNADO_MAX_COLUMNS = 4;

  private final java.util.Random random = new java.util.Random();

  @Override
  public void onFinalWaveTick(Board board, int currentTick) {
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || !tornadoChecked.add(zombie)) {
        continue;
      }
      // فقط زامبی‌هایی که همین حالا از لبهٔ راست وارد شده‌اند
      if (zombie.getX() < board.getColumns() - 0.5 || random.nextDouble() >= TORNADO_CHANCE) {
        continue;
      }
      int jump = MIN_TORNADO_JUMP + random.nextInt(MAX_TORNADO_JUMP - MIN_TORNADO_JUMP + 1);
      zombie.setX(Math.max(0, zombie.getX() - jump));
      System.out.printf(
              "A tornado dropped %s %d column(s) further into the lawn, in lane %d!%n",
              zombie.getName(), jump, zombie.getRow() + 1);
    }
  }

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