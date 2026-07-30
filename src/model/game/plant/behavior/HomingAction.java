package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class HomingAction implements PlantAction {
  private final int actionInterval;
  private final int damage;

  public HomingAction(int actionInterval, int damage) {
    this.actionInterval = actionInterval;
    this.damage = damage;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) return;

    Zombie target = findNearestZombie(board, plant);
    if (target == null) return;

    Projectile projectile = new Projectile(
            damage, 0.5, plant.getCol(), target.getRow(),
            Projectile.ProjectileEffect.NORMAL, false, false, false
    );

    board.addProjectile(projectile);
    plant.setLastActionTick(currentTick);
    System.out.printf("Plant %s fired homing projectile at %s (row %d)!%n",
            plant.getName(), target.getName(), target.getRow());
  }

  private Zombie findNearestZombie(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) continue;

      double distance = Math.hypot(zombie.getRow() - plant.getRow(), zombie.getX() - plant.getCol());
      if (distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}