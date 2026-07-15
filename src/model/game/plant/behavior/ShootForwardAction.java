package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;

public class ShootForwardAction implements PlantAction {
  private int actionInterval;
  private int damage;
  private Projectile.ProjectileEffect effect;
  private boolean piercing;

  public ShootForwardAction(
      int actionInterval, int damage, Projectile.ProjectileEffect effect, boolean piercing) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.effect = effect == null ? Projectile.ProjectileEffect.NORMAL : effect;
    this.piercing = piercing;
  }

  public ShootForwardAction(int actionInterval, int damage, Projectile.ProjectileEffect effect) {
    this(actionInterval, damage, effect, false);
  }

  public ShootForwardAction(int actionInterval, int damage) {
    this(actionInterval, damage, Projectile.ProjectileEffect.NORMAL, false);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() >= actionInterval) {

      if (board.hasZombieInRow(plant.getRow(), plant.getCol())) {

        Projectile projectile =
            new Projectile(
                damage, 0.5, plant.getCol(), plant.getRow(), effect, piercing, false, false);
        board.addProjectile(projectile);

        plant.setLastActionTick(currentTick);
        System.out.printf(
            "Plant %s fired a %s projectile at row %d!%n", plant.getName(), effect, plant.getRow());
      }
    }
  }
}
