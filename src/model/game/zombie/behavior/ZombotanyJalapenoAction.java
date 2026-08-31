package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ZombotanyJalapenoAction implements ZombieAction {
  private final int fuseTicks;
  private final double eatingDamage;
  private int arrivalTick = -1;

  public ZombotanyJalapenoAction(int fuseTicks) {
    this(fuseTicks, 100.0);
  }

  public ZombotanyJalapenoAction(int fuseTicks, double eatingDamage) {
    this.fuseTicks = Math.max(1, fuseTicks);
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (arrivalTick == -1) {
      arrivalTick = currentTick;
    }
    if (currentTick - arrivalTick >= fuseTicks) {
      detonate(zombie, board);
      return;
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

  private void detonate(Zombie zombie, Board board) {
    System.out.printf("%s ignited and torched every plant in row %d!%n",
            zombie.getName(), zombie.getRow() + 1);
    for (Plant plant : board.getPlants()) {
      if (plant.getRow() == zombie.getRow() && !plant.isDead()) {
        plant.takeDamage(10000);
      }
    }
    zombie.takeDamage(zombie.getMaxHealth(), true);
  }
}
