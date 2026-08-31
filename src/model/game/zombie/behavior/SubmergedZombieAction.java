package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class SubmergedZombieAction implements ZombieAction {
  private final double eatingDamage;
  private boolean submerged;

  public SubmergedZombieAction(int unusedSubmergedTicks, int unusedSurfacedTicks,
      double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    // The doc ties diving to the water, not to a clock: it swims where there is sea and walks
    // everywhere else, and it surfaces whenever there is something in front of it to eat.
    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    boolean overWater = board.isWaterAt(zombie.getRow(), (int) Math.round(zombie.getX()));
    submerged = overWater && targetPlant == null;
    zombie.setSubmerged(submerged);

    if (submerged) {
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
}
