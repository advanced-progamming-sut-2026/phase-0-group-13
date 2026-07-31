package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ExplodeAction implements PlantAction {
  private final int fuseTime;
  private final int damage;
  private final int range;
  private final boolean requiresContact;
  private boolean isInitialized;
  private boolean armedMessageShown;

  public ExplodeAction(int fuseTime, int damage, int range, boolean requiresContact) {
    this.fuseTime = fuseTime;
    this.damage = damage;
    this.range = range;
    this.requiresContact = requiresContact;
    this.isInitialized = false;
  }

  public ExplodeAction(int fuseTime, int damage, int range) {
    this(fuseTime, damage, range, fuseTime > 0);
  }

  public ExplodeAction() {
    this(15, 1800, 1);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (!isInitialized) {
      plant.setLastActionTick(currentTick);
      isInitialized = true;
    }

    if (currentTick - plant.getLastActionTick() < fuseTime) {
      return;
    }

    if (!requiresContact) {
      detonateNow(plant, board);
      return;
    }

    if (!armedMessageShown) {
      armedMessageShown = true;
      System.out.printf(
              "%s is armed at (%d, %d) and waiting for a zombie.%n",
              plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    }
    if (hasZombieInRange(plant, board)) {
      detonateNow(plant, board);
    }
  }

  private boolean hasZombieInRange(Plant plant, Board board) {
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()
              && zombie.getRow() == plant.getRow()
              && Math.abs(zombie.getX() - plant.getCol()) <= range) {
        return true;
      }
    }
    return false;
  }

  public void detonateNow(Plant plant, Board board) {
    System.out.printf(
            "BOOM! %s exploded at (%d, %d)%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);

    board.applyAreaDamageToZombies(plant.getCol(), plant.getRow(), range, damage);
    plant.takeDamage(10000);
  }
}
