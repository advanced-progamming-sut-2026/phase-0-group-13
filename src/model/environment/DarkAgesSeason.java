package model.environment;

import java.util.List;
import java.util.Random;
import model.game.Board;
import model.game.GameState;
import model.enums.ZombieType;
import model.game.Tile;
import model.game.TileEffects.TombStoneEffect;
import model.game.zombie.Zombie;

public class DarkAgesSeason extends Season {
  private static final int TOMBSTONE_COUNT = 3;
  private static final int TOMBSTONE_HEALTH = 700;
  private static final int NECROMANCY_INTERVAL_TICKS = 400;
  private static final int MIN_WAVE_GRAVES = 1;
  private static final int MAX_WAVE_GRAVES = 3;
  private static final int REWARD_SUN_CHANCE = 25;
  private static final int REWARD_FOOD_CHANCE = 10;

  private final Random random = new Random();

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
    return rosterOf(ZombieType.JUGGLER, ZombieType.WIZARD, ZombieType.KING,
            ZombieType.IMP_DRAGON);
  }

  @Override
  public String getBossZombieName() {
    return "ZombieZombossMechDark";
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }

  @Override
  public void placeHazards(Board board) {
    for (int i = 0; i < TOMBSTONE_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      int col = 2 + random.nextInt(Math.max(1, board.getColumns() - 4));
      board.placeTileEffect(
              row, col, new TombStoneEffect(TOMBSTONE_HEALTH, true, true, NECROMANCY_INTERVAL_TICKS));
    }
  }
  @Override
  public void onWaveStart(Board board, int waveNumber, int currentTick) {
    spawnWaveGraves(board);
    board.triggerGraveNecromancy(currentTick);
  }

  // داک: در ابتدای هر موجِ زامبی ممکن است چند سنگ‌قبر به‌صورت رندوم به وجود بیایند (مگر اینکه در آن
  private void spawnWaveGraves(Board board) {
    int count = MIN_WAVE_GRAVES + random.nextInt(MAX_WAVE_GRAVES - MIN_WAVE_GRAVES + 1);
    for (int i = 0; i < count; i++) {
      int row = random.nextInt(board.getRows());
      int col = 2 + random.nextInt(Math.max(1, board.getColumns() - 2));
      if (board.getPlantAt(row, col) != null || hasActiveEffect(board, row, col)) {
        continue;
      }
      TombStoneEffect grave = new TombStoneEffect(
              TOMBSTONE_HEALTH, true, random.nextBoolean(), NECROMANCY_INTERVAL_TICKS);
      String reward = rollBuriedReward();
      grave.setBuriedReward(reward);
      board.placeTileEffect(row, col, grave);
      System.out.printf(
              "A grave rose from the ground at (%d, %d)%s.%n",
              col + 1, row + 1, describeReward(reward));
    }
  }

  private boolean hasActiveEffect(Board board, int row, int col) {
    return board.getTile(row, col) != null
            && board.getTile(row, col).getEffect() != null
            && board.getTile(row, col).getEffect().isActive();
  }

  private String rollBuriedReward() {
    int roll = random.nextInt(100);
    if (roll < REWARD_SUN_CHANCE) {
      return "SUN";
    }
    return roll < REWARD_SUN_CHANCE + REWARD_FOOD_CHANCE ? "PLANT_FOOD" : null;
  }

  private String describeReward(String reward) {
    if (reward == null) {
      return "";
    }
    return "SUN".equals(reward) ? " - 50 sun are buried in it" : " - a plant food is buried in it";
  }
}
