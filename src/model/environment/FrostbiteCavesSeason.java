package model.environment;

import java.util.List;
import java.util.Random;
import model.enums.PlantTag;
import model.enums.StatusEffect;
import model.game.Board;
import model.game.GameState;
import model.enums.ZombieType;
import model.game.Tile;
import model.game.TileEffects.IceTrailEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class FrostbiteCavesSeason extends Season {
  private static final int SLIP_TILE_COUNT = 4;
  private static final int FROZEN_TILE_COUNT = 2;
  private static final int HAZARD_DURATION_TICKS = -1;
  private static final int WIND_INTERVAL_TICKS = 200;
  private static final int FREEZE_INCREMENT = 1;
  private static final int FREEZE_DURATION_TICKS = 100;
  private static final int FROZEN_ZOMBIE_COUNT = 2;
  private static final int FROZEN_ZOMBIE_THAW_TICKS = 400;
  // داک: اگر گیاه آتشین در یکی از ۸ خانهٔ اطراف باشد، یخ با نرخ ۶۰ جان بر ثانیه آب می‌شود
  private static final int FIRE_MELT_PER_TICK = 6;

  public static final int WIND_EVENT_TICKS = 28;

  private final Random random = new Random();
  private int lastWindTick = -1;
  private int windRow = -1;
  private int windTick = -1;

  public FrostbiteCavesSeason() {
    this.name = "Frostbite Caves";
  }

  public int getWindRow() {
    return windRow;
  }

  public int getWindTick() {
    return windTick;
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return rosterOf(ZombieType.DODO_RIDER, ZombieType.HUNTER, ZombieType.TROGLOBITE);
  }

  @Override
  public String getBossZombieName() {
    return "ZombieZombossMechCowboy";
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }

  @Override
  public void placeHazards(Board board) {
    // طبق داک، زامبی‌های این فصل با تیر یخی گیاهان یخ نمی‌زنند
    board.setZombiesResistIce(true);
    for (int i = 0; i < SLIP_TILE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = random.nextInt(board.getColumns());
      int laneShift =
              row == 0 ? 1 : (row == board.getRows() - 1 ? -1 : (random.nextBoolean() ? 1 : -1));
      board.placeTileEffect(
              row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.5, false, laneShift));
    }
    for (int i = 0; i < FROZEN_TILE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = random.nextInt(board.getColumns());
      board.placeTileEffect(row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.0, true));
    }
    placeFrozenZombies(board);
  }

  private void placeFrozenZombies(Board board) {
    List<Zombie> pool = getAvailableZombies();
    if (pool.isEmpty()) {
      return;
    }
    for (int i = 0; i < FROZEN_ZOMBIE_COUNT; i++) {
      Zombie frozen = pool.get(random.nextInt(pool.size()));
      if (frozen == null || frozen.isDead()) {
        continue;
      }
      int row = random.nextInt(board.getRows());
      double col = Math.max(0, board.getColumns() - 1 - random.nextInt(3));
      frozen.setRow(row);
      frozen.setX(col);
      frozen.applyEffect(StatusEffect.FROZEN, FROZEN_ZOMBIE_THAW_TICKS);
      board.spawnZombie(frozen);
      System.out.printf(
              "A frozen %s is stuck in the ice at (%d, %d).%n",
              frozen.getName(), (int) col + 1, row + 1);
      pool.remove(frozen);
      if (pool.isEmpty()) {
        return;
      }
    }
  }

  @Override
  public void onTick(Board board, int currentTick) {
    meltIceNearFirePlants(board);

    if (lastWindTick == -1) {
      lastWindTick = currentTick;
      return;
    }
    if (currentTick - lastWindTick < WIND_INTERVAL_TICKS) {
      return;
    }
    lastWindTick = currentTick;

    int targetRow = random.nextInt(Math.max(1, board.getRows()));
    windRow = targetRow;
    windTick = currentTick;
    System.out.printf("A freezing wind sweeps through row %d!%n", targetRow + 1);
    for (Plant plant : board.getPlants()) {
      if (plant.getRow() == targetRow
              && !plant.isDead()
              && !plant.getTags().contains(PlantTag.FIRE)) {
        plant.addFreezeExposure(FREEZE_INCREMENT, currentTick, FREEZE_DURATION_TICKS);
      }
    }
  }

  private void meltIceNearFirePlants(Board board) {
    for (Plant plant : board.getPlants()) {
      if (plant.getIceHealth() <= 0) {
        continue;
      }
      for (Plant other : board.getPlants()) {
        if (other == plant || other.isDead() || !other.getTags().contains(PlantTag.FIRE)) {
          continue;
        }
        if (Math.abs(other.getRow() - plant.getRow()) <= 1
                && Math.abs(other.getCol() - plant.getCol()) <= 1) {
          plant.damageIce(FIRE_MELT_PER_TICK);
          break;
        }
      }
    }
  }
}
