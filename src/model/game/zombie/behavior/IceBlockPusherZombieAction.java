package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class IceBlockPusherZombieAction implements ZombieAction {
  private static final double BLOCK_OFFSET = 1.0;
  private static final double CRUSH_RANGE = 0.6;

  private final double eatingDamage;
  private int blockHealth;

  public IceBlockPusherZombieAction(int blockHealth, double eatingDamage) {
    this.blockHealth = Math.max(1, blockHealth);
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (blockHealth > 0) {
      pushBlock(zombie, board);
      zombie.setEating(false);
      zombie.move();
      return;
    }

    Plant target = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (target != null && !target.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        target.takeDamage((int) eatingDamage);
      }
      return;
    }
    zombie.setEating(false);
    zombie.move();
  }

  private void pushBlock(Zombie zombie, Board board) {
    double blockX = zombie.getX() - BLOCK_OFFSET;
    for (Plant plant : board.getPlants()) {
      if (plant.isDead() || plant.getRow() != zombie.getRow()) {
        continue;
      }
      if (Math.abs(plant.getCol() - blockX) <= CRUSH_RANGE) {
        plant.takeDamage(plant.getMaxHealth());
        blockHealth = 0;
        System.out.printf("%s's ice block crushed %s and shattered!%n",
                zombie.getName(), plant.getName());
        return;
      }
    }
  }

  public boolean hasBlock() {
    return blockHealth > 0;
  }
}
