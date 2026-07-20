package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class SubmergedZombieAction implements ZombieAction {
  private final int submergedTicks;
  private final int surfacedTicks;
  private final double eatingDamage;
  private int phaseStartTick = -1;
  private boolean submerged = true;

  // شنا کردن واقعی (بی‌آسیب بودن زیر آب در برابر حمله‌های سطحی): با zombie.setSubmerged هر تیک وضعیت
  // فعلی رو به خود Zombie اطلاع میده؛ Projectile.hitZombie از این پرچم برای معافیت در برابر
  // شلیک‌های مستقیم استفاده میکنه (فقط پرتابه‌های Lobbed هنوز اثر میکنن)
  public SubmergedZombieAction(int submergedTicks, int surfacedTicks, double eatingDamage) {
    this.submergedTicks = submergedTicks;
    this.surfacedTicks = surfacedTicks;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (phaseStartTick == -1) {
      phaseStartTick = currentTick;
    }

    int phaseDuration = submerged ? submergedTicks : surfacedTicks;
    if (currentTick - phaseStartTick >= phaseDuration) {
      submerged = !submerged;
      phaseStartTick = currentTick;
    }

    zombie.setSubmerged(submerged);

    if (submerged) {
      zombie.setEating(false);
      zombie.move();
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
}