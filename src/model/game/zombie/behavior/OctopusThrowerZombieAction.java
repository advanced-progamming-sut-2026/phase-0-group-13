package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class OctopusThrowerZombieAction implements ZombieAction {
  private static final double THROW_RANGE = 5.0;
  private static final int OCTOPUS_HOLD_TICKS = 120;

  private final int throwIntervalTicks;
  private final double eatingDamage;
  private int lastThrowTick = -1;

  public OctopusThrowerZombieAction(int throwIntervalTicks, double eatingDamage) {
    this.throwIntervalTicks = Math.max(1, throwIntervalTicks);
    this.eatingDamage = eatingDamage;
  }

  /** The tick of its last special action, so the renderer can hold that pose. */
  public int getLastThrowTick() {
    return lastThrowTick;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    throwOctopus(zombie, board, currentTick);

    Plant blocking = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (blocking != null && !blocking.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        blocking.takeDamage((int) eatingDamage);
      }
      return;
    }
    zombie.setEating(false);
    zombie.move();
  }

  private void throwOctopus(Zombie zombie, Board board, int currentTick) {
    if (lastThrowTick != -1 && currentTick - lastThrowTick < throwIntervalTicks) {
      return;
    }
    Plant target = board.getPlantAhead(zombie.getRow(), zombie.getX(), THROW_RANGE);
    if (target == null || target.isDead() || target.isDisabled(currentTick)) {
      return;
    }
    lastThrowTick = currentTick;
    target.disableUntil(currentTick + OCTOPUS_HOLD_TICKS);
    System.out.printf("%s threw an octopus onto %s; it cannot act until the octopus is removed!%n",
            zombie.getName(), target.getName());
  }
}
