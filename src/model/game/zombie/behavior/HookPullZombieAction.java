package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class HookPullZombieAction implements ZombieAction {
  private final int hookInterval;
  private final double eatingDamage;
  private int lastHookTick = -1;

  // برای زامبی‌های آبی (Fisherman/Octopus) که به‌جای خوردن مستقیم، هر hookInterval تیک با قلاب/شاخک
  // یه گیاه رو از ردیف بالا یا پایین خودشون بیرون میکشن و نابود میکنن
  public HookPullZombieAction(int hookInterval, double eatingDamage) {
    this.hookInterval = hookInterval;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastHookTick == -1) {
      lastHookTick = currentTick;
    }
    if (currentTick - lastHookTick >= hookInterval) {
      Plant hooked = board.getPlantAt(zombie.getRow() - 1, zombie.getX());
      if (hooked == null) {
        hooked = board.getPlantAt(zombie.getRow() + 1, zombie.getX());
      }
      if (hooked != null && !hooked.isDead()) {
        hooked.takeDamage(10000);
        System.out.printf("%s hooked and dragged away %s!%n", zombie.getName(), hooked.getName());
      }
      lastHookTick = currentTick;
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