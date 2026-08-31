package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class HunterZombieAction implements ZombieAction {
  private static final double THROW_RANGE = 6.0;
  private static final int FREEZE_DURATION_TICKS = 100;

  private final int throwIntervalTicks;
  private final double eatingDamage;
  private int lastThrowTick = -1;

  public HunterZombieAction(int throwIntervalTicks, double eatingDamage) {
    this.throwIntervalTicks = Math.max(1, throwIntervalTicks);
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    throwIce(zombie, board, currentTick);

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

  private void throwIce(Zombie zombie, Board board, int currentTick) {
    if (lastThrowTick != -1 && currentTick - lastThrowTick < throwIntervalTicks) {
      return;
    }
    Plant target = board.getPlantAhead(zombie.getRow(), zombie.getX(), THROW_RANGE);
    if (target == null || target.isDead() || target.isFrozen(currentTick)) {
      return;
    }
    lastThrowTick = currentTick;
    target.addFreezeExposure(1, currentTick, FREEZE_DURATION_TICKS);

    if (target.isFrozen(currentTick)) {
      System.out.printf("%s froze %s solid!%n", zombie.getName(), target.getName());
    } else {
      System.out.printf("%s threw ice at %s (freeze level %d/%d).%n",
              zombie.getName(), target.getName(), target.getFreezeLevel(), Plant.MAX_FREEZE_LEVEL);
    }
  }
}
