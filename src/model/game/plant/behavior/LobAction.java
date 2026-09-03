package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class LobAction implements PlantAction {

  static final double LOB_SPEED = 0.25;
  /** How far a fire lobber's heat reaches, in tiles. */
  private static final int WARM_RADIUS = 1;
  private static final double SPLASH_TILES = 1.0;

  private final int actionInterval;
  private final int damage;
  private final boolean aoe;
  private final Projectile.ProjectileEffect effect;

  public LobAction(int actionInterval, int damage, boolean aoe, Projectile.ProjectileEffect effect) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.aoe = aoe;
    this.effect = effect == null ? Projectile.ProjectileEffect.NORMAL : effect;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) return;

    Zombie target = findNearestZombieAhead(board, plant);
    if (target == null) return;

    Projectile shot = lob(plant, target, damage, effect);
    if (aoe) {
      shot.withSplash(SPLASH_TILES);
    }
    board.addProjectile(shot);

    if (effect == Projectile.ProjectileEffect.FIRE) {
      // Pepper-pult "also warms the surrounding tiles", which until now it did not.
      board.warmTiles(plant.getRow(), plant.getCol(), WARM_RADIUS, plant);
    }
    plant.setLastActionTick(currentTick);
    System.out.printf("Plant %s lobbed a %s projectile at row %d!%n", plant.getName(), effect, plant.getRow() + 1);
  }

  static Projectile lob(Plant plant, Zombie target, int damage,
          Projectile.ProjectileEffect effect) {
    return new Projectile(damage, LOB_SPEED, plant.getCol(), plant.getRow(), effect,
            false, true, false)
            .firedBy(plant.getName())
            .aimedAt(target.getX());
  }

  static Zombie findNearestZombieAhead(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow() || zombie.getX() < plant.getCol()) continue;

      double distance = zombie.getX() - plant.getCol();
      if (distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
