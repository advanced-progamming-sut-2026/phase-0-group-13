package model.game.plant.behavior;

import java.util.Random;
import model.enums.StatusEffect;
import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ButterLobAction implements PlantAction {

  public static final int BASE_BUTTER_CHANCE_PERCENT = 25;
  private static final int STUN_TICKS = 30;

  private final int actionInterval;
  private final int kernelDamage;
  private final int butterDamage;
  private final int butterChancePercent;
  private final boolean alwaysButter;
  private final Random random = new Random();

  public ButterLobAction(int actionInterval, int kernelDamage, int butterDamage,
                         int butterChancePercent, boolean alwaysButter) {
    this.actionInterval = Math.max(1, actionInterval);
    this.kernelDamage = kernelDamage;
    this.butterDamage = butterDamage;
    this.butterChancePercent = Math.max(0, Math.min(100, butterChancePercent));
    this.alwaysButter = alwaysButter;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    if (alwaysButter) {
      int buttered = 0;
      for (Zombie zombie : board.getZombies()) {
        if (!zombie.isDead() && !zombie.isHypnotized()) {
          hitWithButter(zombie);
          buttered++;
        }
      }
      if (buttered > 0) {
        plant.setLastActionTick(currentTick);
        System.out.printf("%s dropped butter on %d zombie(s).%n", plant.getName(), buttered);
      }
      return;
    }

    Zombie target = LobAction.findNearestZombieAhead(board, plant);
    if (target == null) {
      return;
    }
    boolean butter = random.nextInt(100) < butterChancePercent;
    Projectile shot = LobAction.lob(plant, target, butter ? butterDamage : kernelDamage,
            Projectile.ProjectileEffect.NORMAL);
    if (butter) {
      shot.withStun(STUN_TICKS);
    }
    board.addProjectile(shot);
    System.out.printf("%s lobbed %s at %s.%n", plant.getName(),
            butter ? "butter" : "a kernel", target.getName());
    plant.setLastActionTick(currentTick);
  }

  private void hitWithButter(Zombie zombie) {
    zombie.takeDamage(butterDamage, false);
    zombie.setEating(false);
    zombie.applyEffect(StatusEffect.FROZEN, STUN_TICKS);
  }
}
