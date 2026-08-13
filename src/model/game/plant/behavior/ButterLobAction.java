package model.game.plant.behavior;

import java.util.Random;
import model.enums.StatusEffect;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * Kernel-pult: طبق plants.json دمیجش "20/40" است و «یک دانهٔ ذرت (دمیج کم) یا کره (که زامبی را
 * کوتاه‌مدت گیج می‌کند)» پرتاب می‌کند. قبلا فقط LobAction با دمیج ۲۰ بود و کره اصلا وجود نداشت.
 *
 * <p>«Butter +5%» در Lvl 2 یعنی شانس کره پایه‌ای دارد؛ خود دیتا عدد پایه را ننوشته، پس
 * {@link #BASE_BUTTER_CHANCE_PERCENT} به عنوان پایه گرفته شده و بونوس لِوِل رویش سوار می‌شود.
 *
 * <p>گیج‌شدن با StatusEffect.FROZEN مدل شده چون تنها حالتی است که در
 * {@link Zombie#update} جلوی حرکت و حملهٔ زامبی را می‌گیرد؛ حالت جدیدی اضافه نشده.
 */
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
      // اثر غذای گیاه: «روی سر هر زامبیِ روی زمین کره می‌اندازد»
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

    Zombie target = findNearestZombieAhead(board, plant);
    if (target == null) {
      return;
    }
    if (random.nextInt(100) < butterChancePercent) {
      hitWithButter(target);
      System.out.printf("%s buttered %s, stunning it!%n", plant.getName(), target.getName());
    } else {
      target.takeDamage(kernelDamage, false);
      System.out.printf("%s lobbed a kernel at %s.%n", plant.getName(), target.getName());
    }
    plant.setLastActionTick(currentTick);
  }

  private void hitWithButter(Zombie zombie) {
    zombie.takeDamage(butterDamage, false);
    zombie.setEating(false);
    zombie.applyEffect(StatusEffect.FROZEN, STUN_TICKS);
  }

  private Zombie findNearestZombieAhead(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow()
              || zombie.getX() < plant.getCol()) {
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
