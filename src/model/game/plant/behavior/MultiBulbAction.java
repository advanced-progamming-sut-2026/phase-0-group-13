package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;

public class MultiBulbAction implements PlantAction {

  private final String[] names;
  private final int[] damages;
  private final int[] intervals;
  private final int ricochetLifeTicks;
  private final int[] lastFiredTick;

  public MultiBulbAction(String[] names, int[] damages, int[] intervals, int ricochetLifeTicks) {
    this.names = names;
    this.damages = damages;
    this.intervals = intervals;
    this.ricochetLifeTicks = Math.max(1, ricochetLifeTicks);
    this.lastFiredTick = new int[damages.length];
    java.util.Arrays.fill(this.lastFiredTick, Integer.MIN_VALUE);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (!board.hasZombieInRow(plant.getRow(), plant.getCol())) {
      return;
    }

    for (int i = 0; i < damages.length; i++) {
      if (lastFiredTick[i] != Integer.MIN_VALUE && currentTick - lastFiredTick[i] < intervals[i]) {
        continue;
      }
      fireBulb(plant, board, i);
      lastFiredTick[i] = currentTick;
      plant.setLastActionTick(currentTick);
    }
  }

  private void fireBulb(Plant plant, Board board, int index) {
    Projectile bulb = new Projectile(damages[index], 0.5, plant.getCol(), plant.getRow(),
            Projectile.ProjectileEffect.NORMAL, false, false, false);
    bulb.firedBy(plant.getName());
    bulb.setDirection(1, 1);
    bulb.makeBouncing(ricochetLifeTicks);
    board.addProjectile(bulb);
    System.out.printf("%s rolled a %s bulb (%d dmg) down row %d!%n",
            plant.getName(), names[index], damages[index], plant.getRow() + 1);
  }
}
