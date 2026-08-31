package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class LaneMagnetAction implements PlantAction {
  private static final double PULL_RANGE = 3.0;

  private final int pullIntervalTicks;
  private final boolean pullAllAndHeal;
  private int lastPullTick = -1;

  public LaneMagnetAction(int pullIntervalTicks) {
    this(pullIntervalTicks, false);
  }

  public LaneMagnetAction(int pullIntervalTicks, boolean pullAllAndHeal) {
    this.pullIntervalTicks = Math.max(1, pullIntervalTicks);
    this.pullAllAndHeal = pullAllAndHeal;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (lastPullTick != -1 && currentTick - lastPullTick < pullIntervalTicks) {
      return;
    }

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.isHypnotized() || zombie.getRow() == plant.getRow()) {
        continue;
      }
      if (Math.abs(zombie.getRow() - plant.getRow()) != 1
              || zombie.getX() - plant.getCol() > PULL_RANGE
              || zombie.getX() < plant.getCol()) {
        continue;
      }

      int fromRow = zombie.getRow();
      zombie.setRow(plant.getRow());
      zombie.setEating(false);
      lastPullTick = currentTick;
      plant.setLastActionTick(currentTick);
      System.out.printf("%s magnetised %s from row %d into row %d!%n",
              plant.getName(), zombie.getName(), fromRow + 1, plant.getRow() + 1);
      if (!pullAllAndHeal) {
        return;
      }
    }

    if (pullAllAndHeal) {
      plant.heal(plant.getMaxHealth());
    }
  }
}
