package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class DodoRiderZombieAction implements ZombieAction {
  private static final String TALL_NUT_NAME = "Tall-nut";

  /** The doc's "obstacle": a nut-class plant it hops rather than eats. Tall-nut is too high. */
  private static final int OBSTACLE_HEALTH = 1000;

  private boolean grounded;
  private final double eatingDamage;

  public DodoRiderZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead() && isTallNut(targetPlant) && !grounded) {
      grounded = true;
      System.out.printf("%s's Dodo was forced to land by a %s!%n",
              zombie.getName(), targetPlant.getName());
    }
    if (!grounded && targetPlant != null && !targetPlant.isDead() && isObstacle(targetPlant)) {
      zombie.setEating(false);
      zombie.move();
      return;
    }

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

  private boolean isTallNut(Plant plant) {
    return TALL_NUT_NAME.equalsIgnoreCase(plant.getName());
  }

  private boolean isObstacle(Plant plant) {
    return !isTallNut(plant)
            && (plant.getMaxHealth() >= OBSTACLE_HEALTH
            || plant.getTags().contains(model.enums.PlantTag.EXPLOSIVE));
  }
}
