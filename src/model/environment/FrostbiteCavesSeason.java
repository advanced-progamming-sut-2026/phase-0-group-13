package model.environment;

import java.util.List;
import java.util.Random;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.TileEffects.IceTrailEffect;
import model.game.zombie.Zombie;

public class FrostbiteCavesSeason extends Season {
  private static final int SLIP_TILE_COUNT = 4;
  private static final int FROZEN_TILE_COUNT = 2;
  private static final int HAZARD_DURATION_TICKS = -1; // -1 یعنی دائمی، مثل خود Tombstone

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
    Random random = new Random();
    for (int i = 0; i < SLIP_TILE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = random.nextInt(board.getColumns());
      board.placeTileEffect(row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.5, false));
    }
    for (int i = 0; i < FROZEN_TILE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = random.nextInt(board.getColumns());
      board.placeTileEffect(row, col, new IceTrailEffect(HAZARD_DURATION_TICKS, 0.0, true));
    }
  }
}
