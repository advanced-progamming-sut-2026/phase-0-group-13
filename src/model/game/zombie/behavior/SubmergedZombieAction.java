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

  // شنا کردن واقعی (بی‌آسیب بودن زیر آب در برابر حمله‌های سطحی) نیاز به تغییر تو Projectile/Board
  // داره که فعلا نیست؛ اینجا فقط الگوی حرکتی شبیه‌سازی میشه: زیر آب سریع‌تر جلو میره و کاری به گیاه
  // نداره، رو سطح مثل زامبی عادی گیاه میخوره
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

    if (submerged) {
      zombie.setEating(false);
      zombie.move();
      return;
    }

    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
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
