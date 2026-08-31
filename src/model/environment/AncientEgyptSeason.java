package model.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import model.game.Board;
import model.game.GameState;
import model.enums.ZombieType;
import model.game.Tile;
import model.game.zombie.Zombie;

public class AncientEgyptSeason extends Season {
  // داک: «فقط در موج آخر» ممکن است زامبی به‌جای ورود عادی، با گردباد و بین ۱ تا ۴ ستون جلوتر از
  private static final double TORNADO_CHANCE = 0.5;
  private static final int MIN_TORNADO_JUMP = 1;
  private static final int MAX_TORNADO_JUMP = 4;

  public static final int TORNADO_EVENT_TICKS = 22;
  private static final int MAX_REMEMBERED_TORNADOES = 24;

  public record TornadoEvent(int row, double fromColumn, double toColumn, int tick) {}

  private final Random random = new Random();
  private final Set<Zombie> tornadoChecked =
          Collections.newSetFromMap(new IdentityHashMap<Zombie, Boolean>());
  private final List<TornadoEvent> tornadoes = new ArrayList<>();

  public AncientEgyptSeason() {
    this.name = "Ancient Egypt";
  }

  public List<TornadoEvent> getRecentTornadoes() {
    return Collections.unmodifiableList(tornadoes);
  }

  private void recordTornado(Zombie zombie, double fromColumn, int currentTick) {
    tornadoes.removeIf(event -> currentTick - event.tick() > TORNADO_EVENT_TICKS);
    if (tornadoes.size() >= MAX_REMEMBERED_TORNADOES) {
      tornadoes.remove(0);
    }
    tornadoes.add(new TornadoEvent(zombie.getRow(), fromColumn, zombie.getX(), currentTick));
  }

  private static final int TORNADO_MIN_COLUMNS = 1;
  private static final int TORNADO_MAX_COLUMNS = 4;


  @Override
  public void onFinalWaveTick(Board board, int currentTick) {
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || !tornadoChecked.add(zombie)) {
        continue;
      }
      if (zombie.getX() < board.getColumns() - 0.5 || random.nextDouble() >= TORNADO_CHANCE) {
        continue;
      }
      int jump = MIN_TORNADO_JUMP + random.nextInt(MAX_TORNADO_JUMP - MIN_TORNADO_JUMP + 1);
      double from = zombie.getX();
      zombie.setX(Math.max(0, zombie.getX() - jump));
      recordTornado(zombie, from, currentTick);
      System.out.printf(
              "A tornado dropped %s %d column(s) further into the lawn, in lane %d!%n",
              zombie.getName(), jump, zombie.getRow() + 1);
    }
  }

  @Override
  public void placeHazards(model.game.Board board) {
    board.placeRandomTombstones(3, 5, 700);
  }

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
      double from = zombie.getX();
      zombie.setX(Math.max(0, zombie.getX() - jump));
      recordTornado(zombie, from, currentTick);
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
    return rosterOf(ZombieType.RA, ZombieType.EXPLORER, ZombieType.TOMBRAISER);
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
