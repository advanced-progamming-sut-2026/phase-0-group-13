package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class WizardZombieAction implements ZombieAction {

  private final double eatingDamage;
  private int lastCurseTick = -1;

  public WizardZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  /** The tick it last cast, so the renderer can hold the spell pose. */
  public int getLastCurseTick() {
    return lastCurseTick;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getTopPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead() && !targetPlant.isCursed()) {
      targetPlant.applyCurse(zombie);
      lastCurseTick = currentTick;
      System.out.printf(
              "%s turned %s into a harmless sheep!%n", zombie.getName(), targetPlant.getName());
    }

    // It then has to chew its way through like anything else. Cursing and walking on meant the
    // Wizard was the one zombie in the roster no barricade could stop -- it strolled the length
    // of the lane leaving a row of sheep behind it and never took a bite of anything.
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
      return;
    }
    zombie.setEating(false);
    zombie.move();
  }
}
