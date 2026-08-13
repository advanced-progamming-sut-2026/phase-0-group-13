package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;

/**
 * Magnet-shroom: طبق plants.json دمیجش صفر است و کارش «کندنِ وسایل فلزی (سطل/کلاهخود) از سر
 * زامبی‌هاست»، نه شلیک. قبلا با HomingAction و دمیج صفر ساخته می‌شد، یعنی عملا هیچ کاری نمی‌کرد.
 *
 * <p>زره فلزی را نابود می‌کند (همان مسیر Armor.takeDamage که تیرها هم استفاده می‌کنند)، پس جانِ
 * بدنِ زامبی دست‌نخورده می‌ماند و فقط بی‌زره می‌شود.
 */
public class MagnetAction implements PlantAction {

  private final int actionInterval;
  private final int maxTargets;
  private final double range;

  public MagnetAction(int actionInterval, int maxTargets, double range) {
    this.actionInterval = Math.max(1, actionInterval);
    this.maxTargets = Math.max(1, maxTargets);
    this.range = range;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    int stripped = 0;
    for (Zombie zombie : board.getZombies()) {
      if (stripped >= maxTargets) {
        break;
      }
      if (zombie.isDead() || zombie.isHypnotized() || !inRange(plant, zombie)) {
        continue;
      }
      if (stripMetal(plant, zombie)) {
        stripped++;
      }
    }

    if (stripped > 0) {
      plant.setLastActionTick(currentTick);
    }
  }

  private boolean inRange(Plant plant, Zombie zombie) {
    return zombie.getRow() == plant.getRow() && zombie.getX() - plant.getCol() <= range;
  }

  private boolean stripMetal(Plant plant, Zombie zombie) {
    for (Armor armor : zombie.getArmors()) {
      if (armor.isMetallic() && !armor.isDestroyed()) {
        armor.takeDamage(armor.getCurrentHealth());
        System.out.printf("%s pulled the %s off %s!%n",
                plant.getName(), armor.getType(), zombie.getName());
        return true;
      }
    }
    return false;
  }
}
