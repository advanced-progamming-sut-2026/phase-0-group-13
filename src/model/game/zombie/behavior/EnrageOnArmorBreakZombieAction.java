package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class EnrageOnArmorBreakZombieAction implements ZombieAction {
  private static final int NORMAL_BITE_INTERVAL_TICKS = 10;
  private final double eatingDamage;
  private final double enragedSpeedMultiplier;
  private final int enragedBiteIntervalTicks;
  private boolean enraged = false;

  public EnrageOnArmorBreakZombieAction(double eatingDamage, double enragedSpeedMultiplier) {
    this(eatingDamage, enragedSpeedMultiplier, NORMAL_BITE_INTERVAL_TICKS / 2);
  }

  public EnrageOnArmorBreakZombieAction(
          double eatingDamage, double enragedSpeedMultiplier, int enragedBiteIntervalTicks) {
    this.eatingDamage = eatingDamage;
    this.enragedSpeedMultiplier = enragedSpeedMultiplier;
    this.enragedBiteIntervalTicks = Math.max(1, enragedBiteIntervalTicks);
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!enraged && !zombie.hasIntactArmor()) {
      enraged = true;
      zombie.setSpeedMultiplier(enragedSpeedMultiplier);
      System.out.printf("%s lost its newspaper and got enraged!%n", zombie.getName());
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      int biteInterval = enraged ? enragedBiteIntervalTicks : NORMAL_BITE_INTERVAL_TICKS;
      if (currentTick % biteInterval == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}