package model.environment;

import java.util.List;
import java.util.Random;
import model.enums.PlantTag;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.TileEffects.IceTrailEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class FrostbiteCavesSeason extends Season {
  private static final int SLIP_TILE_COUNT = 4;
  private static final int FROZEN_TILE_COUNT = 2;
  private static final int HAZARD_DURATION_TICKS = -1; // -1 یعنی دائمی، مثل خود Tombstone
  private static final int WIND_INTERVAL_TICKS = 200;
  private static final int FREEZE_INCREMENT = 34;
  private static final int FREEZE_DURATION_TICKS = 100;

  private final Random random = new Random();
  private int lastWindTick = -1;

  public FrostbiteCavesSeason() {
    this.name = "Frostbite Caves";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return zombiesByAliasKeyword("iceage");
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }

  // چند خونه لیزخوردن (فقط کند میکنن) و چند خونه کاملا یخ‌زده (زامبی روشون کاملا فریز میشه) رندوم رو
  // نقشه میزاره
  @Override
  public void placeHazards(Board board) {
    Random hazardRandom = new Random();
    for (int i = 0; i < SLIP_TILE_COUNT; i++) {
      int row = hazardRandom.nextInt(board.getRows());
      int col = hazardRandom.nextInt(board.getColumns());
      board.placeTileEffect(row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.5, false));
    }
    for (int i = 0; i < FROZEN_TILE_COUNT; i++) {
      int row = hazardRandom.nextInt(board.getRows());
      int col = hazardRandom.nextInt(board.getColumns());
      board.placeTileEffect(row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.0, true));
    }
  }

  @Override
  public void onTick(Board board, int currentTick) {
    if (lastWindTick == -1) {
      lastWindTick = currentTick;
      return;
    }
    if (currentTick - lastWindTick < WIND_INTERVAL_TICKS) {
      return;
    }
    lastWindTick = currentTick;

    int targetRow = random.nextInt(Math.max(1, board.getRows()));
    System.out.printf("A freezing wind sweeps through row %d!%n", targetRow + 1);
    for (Plant plant : board.getPlants()) {
      if (plant.getRow() == targetRow
              && !plant.isDead()
              && !plant.getTags().contains(PlantTag.FIRE)) {
        plant.addFreezeExposure(FREEZE_INCREMENT, currentTick, FREEZE_DURATION_TICKS);
      }
    }
  }
}