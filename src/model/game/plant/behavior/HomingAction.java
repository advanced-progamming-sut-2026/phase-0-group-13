package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class HomingAction implements PlantAction {
  private int actionInterval;
  private int damage;

  public HomingAction(int actionInterval, int damage) {
    this.actionInterval = actionInterval;
    this.damage = damage;
  }

  // Projectile فعلی فقط تو یه ردیف ثابت حرکت میکنه (نمیتونه دنبال زامبی بره)، پس اینجا فقط لحظه
  // شلیک نزدیک‌ترین زامبی رو تو کل نقشه پیدا میکنیم و رو ردیف همون هدف شلیک میکنیم؛ ردیابی واقعی
  // لحظه‌به‌لحظه نیازمند تغییر تو خود Projectile هستش که فعلا انجام نشده
  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    Zombie target = findNearestZombie(board, plant);
    if (target == null) {
      return;
    }

    Projectile projectile =
        new Projectile(
            damage,
            0.5,
            plant.getCol(),
            target.getRow(),
            Projectile.ProjectileEffect.NORMAL,
            false,
            false,
            false);
    board.addProjectile(projectile);

    plant.setLastActionTick(currentTick);
    System.out.printf(
        "Plant %s fired a homing projectile at %s (row %d)!%n",
        plant.getName(), target.getName(), target.getRow());
  }

  private Zombie findNearestZombie(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      double rowDiff = zombie.getRow() - plant.getRow();
      double colDiff = zombie.getX() - plant.getCol();
      double distance = Math.hypot(rowDiff, colDiff);
      if (distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
