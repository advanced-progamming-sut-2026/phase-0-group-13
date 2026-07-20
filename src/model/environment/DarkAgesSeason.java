package model.environment;

import java.util.List;
import java.util.Random;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.TileEffects.TombStoneEffect;
import model.game.zombie.Zombie;

public class DarkAgesSeason extends Season {
  private static final int TOMBSTONE_COUNT = 3;
  private static final int TOMBSTONE_HEALTH = 700;
  private static final int NECROMANCY_INTERVAL_TICKS = 400;

  public DarkAgesSeason() {
    this.name = "Dark Ages";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
    gameState.setSkySunDisabled(true);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return zombiesByAliasKeyword("dark");
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }

  // چند تا سنگ‌قبر رندوم روی نقشه میزاره؛ هرکدوم ۷۰۰ HP داره، جلوی تیر گیاهی رو میگیره (تا وقتی
  // نابود بشه)، و هر ۴۰ ثانیه یه زامبی از دلش زنده میکنه (نکرومنسی)
  @Override
  public void placeHazards(Board board) {
    Random random = new Random();
    for (int i = 0; i < TOMBSTONE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = 2 + random.nextInt(Math.max(1, board.getColumns() - 4));
      board.placeTileEffect(
              row, col, new TombStoneEffect(TOMBSTONE_HEALTH, true, true, NECROMANCY_INTERVAL_TICKS));
    }
  }
  @Override
  public void onWaveStart(Board board, int waveNumber, int currentTick) {
    board.triggerGraveNecromancy(currentTick);
  }
}