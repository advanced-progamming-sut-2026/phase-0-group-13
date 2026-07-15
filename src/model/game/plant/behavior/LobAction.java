package model.game.plant.behavior;

import model.enums.StatusEffect;
import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class LobAction implements PlantAction {
  private int actionInterval;
  private int damage;
  private boolean aoe;
  private Projectile.ProjectileEffect effect;

  public LobAction(int actionInterval, int damage, boolean aoe, Projectile.ProjectileEffect effect) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.aoe = aoe;
    this.effect = effect == null ? Projectile.ProjectileEffect.NORMAL : effect;
  }

  // پرتاب هوایی: بر خلاف شوتر از روی دیوار/سپر رد میشه چون به‌جای Projectile مستقیم زامبی هدف رو
  // میزنه، پس hasShieldBlocker چک نمیشه
  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    Zombie target = findNearestZombieAhead(board, plant);
    if (target == null) {
      return;
    }

    applyDamage(target);
    if (aoe) {
      for (Zombie zombie : board.getZombies()) {
        if (zombie != target
            && !zombie.isDead()
            && zombie.getRow() == target.getRow()
            && Math.abs(zombie.getX() - target.getX()) <= 1.0) {
          applyDamage(zombie);
        }
      }
    }

    plant.setLastActionTick(currentTick);
    System.out.printf(
        "Plant %s lobbed a %s projectile at row %d!%n", plant.getName(), effect, plant.getRow());
  }

  private void applyDamage(Zombie zombie) {
    int finalDamage = effect == Projectile.ProjectileEffect.FIRE ? damage * 2 : damage;
    zombie.takeDamage(finalDamage, effect == Projectile.ProjectileEffect.POISON);

    if (effect == Projectile.ProjectileEffect.ICE) {
      zombie.applyEffect(StatusEffect.CHILLED, 50);
    }
    if (effect == Projectile.ProjectileEffect.FIRE) {
      zombie.extinguishFrozenStatus();
    }
  }

  private Zombie findNearestZombieAhead(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow() || zombie.getX() < plant.getCol()) {
        continue;
      }
      double distance = zombie.getX() - plant.getCol();
      if (distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
