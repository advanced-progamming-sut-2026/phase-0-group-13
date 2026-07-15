package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class MeleeAction implements PlantAction {
  private static final double MELEE_RANGE = 1.5;

  private int actionInterval;
  private int damage;

  public MeleeAction(int actionInterval, int damage) {
    this.actionInterval = actionInterval;
    this.damage = damage;
  }

  // برخلاف شوتر/لوبر، ملی هیچ Projectile نمیسازه؛ مستقیم به نزدیک‌ترین زامبی تو برد نزدیک ضربه میزنه
  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    Zombie target = findAdjacentZombie(board, plant);
    if (target == null) {
      return;
    }

    target.takeDamage(damage, false);
    plant.setLastActionTick(currentTick);
    System.out.printf(
        "Plant %s hit %s in melee range!%n", plant.getName(), target.getName());
  }

  private Zombie findAdjacentZombie(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow()) {
        continue;
      }
      double distance = zombie.getX() - plant.getCol();
      if (distance >= 0 && distance <= MELEE_RANGE && distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
