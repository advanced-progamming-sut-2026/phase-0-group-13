package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class RangedDemolisherZombieAction implements ZombieAction {
  private final int fuseTicks;
  private final double range;
  private int fuseStartTick = -1;

  // پراسپکتور به‌جای خوردن گیاه، از فاصله دینامیت پرت میکنه؛ وقتی گیاهی تو برد باشه یه فیوز شروع
  // میشه و بعد از fuseTicks گیاه هدف مستقیم نابود میشه، بدون اینکه زامبی نزدیکش بشه
  public RangedDemolisherZombieAction(int fuseTicks, double range) {
    this.fuseTicks = fuseTicks;
    this.range = range;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAhead(zombie.getRow(), zombie.getX(), range);

    if (targetPlant == null || targetPlant.isDead()) {
      fuseStartTick = -1;
      zombie.setEating(false);
      zombie.move();
      return;
    }

    if (fuseStartTick == -1) {
      fuseStartTick = currentTick;
    }

    zombie.setEating(false);

    if (currentTick - fuseStartTick >= fuseTicks) {
      targetPlant.takeDamage(10000);
      System.out.printf(
          "%s blew up %s with dynamite!%n", zombie.getName(), targetPlant.getName());
      fuseStartTick = -1;
    }
  }
}
