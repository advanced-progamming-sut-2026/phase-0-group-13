package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ExplodeAction implements PlantAction {
  private final int fuseTime;
  private final int damage;
  private final int range;
  private boolean isInitialized;

  public ExplodeAction(int fuseTime, int damage, int range) {
    this.fuseTime = fuseTime;
    this.damage = damage;
    this.range = range;
    this.isInitialized = false;
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

    if (currentTick - plant.getLastActionTick() >= fuseTime) {
      detonateNow(plant, board);
    }
  }

  public void detonateNow(Plant plant, Board board) {
    System.out.printf("BOOM! %s exploded at (%d, %d)%n", plant.getName(), plant.getCol(), plant.getRow());

    for (Zombie zombie : board.getZombies()) {
      if (zombie.getRow() == plant.getRow() && Math.abs(zombie.getX() - plant.getCol()) <= range) {
        zombie.takeDamage(damage, false);
      }
    }
    plant.takeDamage(10000);
  }
}