package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class DodoRiderZombieAction implements ZombieAction {
  private static final String TALL_NUT_NAME = "Tall-nut";

  private boolean isFlying = true;
  private final double eatingDamage;

  // FIX (GDD Target 1.3): قبلا هر گیاهی رو با ۱۰۰۰۰ دمیج نابود میکرد و بعدش پیاده میشد. حالا از رو
  // گیاه‌های معمولی/دفاعی بی‌آسیب پرواز میکنه رد میشه (اصلا بهشون آسیب نمیزنه) و فقط Tall-nut
  // مجبورش میکنه فرود بیاد و مثل زامبی عادی شروع به خوردنش کنه
  public DodoRiderZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (isFlying) {
      Plant blockingPlant = board.getPlantAhead(zombie.getRow(), zombie.getX(), 1.0);
      if (blockingPlant != null && !blockingPlant.isDead() && isTallNut(blockingPlant)) {
        isFlying = false;
        System.out.printf(
                "%s's Dodo was forced to land by a %s!%n", zombie.getName(), blockingPlant.getName());
      } else {
        zombie.setEating(false);
        zombie.move();
        return;
      }
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

  private boolean isTallNut(Plant plant) {
    return TALL_NUT_NAME.equalsIgnoreCase(plant.getName());
  }
}