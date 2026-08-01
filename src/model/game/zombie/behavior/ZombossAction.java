package model.game.zombie.behavior;

import data.GameDataManager;
import java.util.Random;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

// رفتار دکتر زامباس تو مرحله‌ی باس: آروم جلو میاد، گیاه سر راهش رو له میکنه و هر چند ثانیه یه بار
// هم یه زامبی کمکی احضار میکنه و هم با کوبیدن دستش یه ستون تصادفی رو داغون میکنه
public class ZombossAction implements ZombieAction {
  private static final String MINION_ALIAS = "ZombieEgyptImpDefault";

  private final int summonIntervalTicks;
  private final int smashDamage;
  private final double eatingDamage;
  private final Random random = new Random();
  private int lastSummonTick = -1;

  public ZombossAction(int summonIntervalTicks, int smashDamage, double eatingDamage) {
    this.summonIntervalTicks = Math.max(1, summonIntervalTicks);
    this.smashDamage = smashDamage;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastSummonTick == -1) {
      lastSummonTick = currentTick;
    }
    if (currentTick - lastSummonTick >= summonIntervalTicks) {
      lastSummonTick = currentTick;
      summonMinion(zombie, board);
      smashRandomColumn(zombie, board);
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }

  private void summonMinion(Zombie zombie, Board board) {
    ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
    int lane = board.getRows() > 0 ? random.nextInt(board.getRows()) : zombie.getRow();
    Zombie minion = factory.createZombie(MINION_ALIAS, lane, board.getColumns());
    if (minion != null) {
      board.spawnZombie(minion);
      System.out.printf("Dr. Zomboss summoned reinforcements in row %d!%n", lane + 1);
    }
  }

  private void smashRandomColumn(Zombie zombie, Board board) {
    if (board.getRows() <= 0 || board.getColumns() <= 0) {
      return;
    }
    int row = random.nextInt(board.getRows());
    int col = random.nextInt(board.getColumns());
    System.out.printf("Dr. Zomboss slammed the lawn at (%d, %d)!%n", col + 1, row + 1);
    board.applyAreaDamageToPlants(col, row, 1, smashDamage);
  }
}
