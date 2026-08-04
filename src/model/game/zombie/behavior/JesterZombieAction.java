package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;


public class JesterZombieAction implements ZombieAction {
  private static final double CATCH_RANGE = 0.6;
  private static final int SPIN_DURATION_TICKS = 30;
  private static final double SPIN_SPEED_MULTIPLIER = 2.0;

  private final double eatingDamage;
  private int spinTicksLeft;

  public JesterZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    reflectIncomingProjectiles(zombie, board);
    updateSpin(zombie);
    walkOrEat(zombie, board, currentTick);
  }

  /**
   * ژانگولر تا وقتی پرتابه‌ای به سمتش می‌آید می‌چرخد و در حال چرخش سریع‌تر جلو می‌رود؛
   * وقتی مدتی پرتابه‌ای نیاید، چرخش تمام می‌شود و به سرعت عادی برمی‌گردد.
   */
  private void updateSpin(Zombie zombie) {
    if (spinTicksLeft <= 0) {
      return;
    }
    spinTicksLeft--;
    if (spinTicksLeft == 0) {
      zombie.setSpeedMultiplier(1.0);
      System.out.printf("%s stopped spinning.%n", zombie.getName());
    }
  }

  private void startSpinning(Zombie zombie) {
    if (spinTicksLeft <= 0) {
      zombie.setSpeedMultiplier(SPIN_SPEED_MULTIPLIER);
      System.out.printf("%s started spinning and speeds up!%n", zombie.getName());
    }
    spinTicksLeft = SPIN_DURATION_TICKS;
  }

  private void reflectIncomingProjectiles(Zombie zombie, Board board) {
    // addProjectile وسط پیمایش همون لیست، ConcurrentModificationException میداد
    List<Projectile> reflected = new ArrayList<>();
    Iterator<Projectile> iterator = board.getProjectiles().iterator();
    while (iterator.hasNext()) {
      Projectile incoming = iterator.next();
      if (!canReflect(incoming, zombie)) {
        continue;
      }
      iterator.remove();
      reflected.add(buildReflectedProjectile(incoming, zombie));
      startSpinning(zombie);
      System.out.printf("%s juggled a projectile back at the plants!%n", zombie.getName());
    }
    for (Projectile projectile : reflected) {
      board.addProjectile(projectile);
    }
  }

  private boolean canReflect(Projectile incoming, Zombie zombie) {
    return incoming.isActive()
            && !incoming.isFromZombie()
            && !incoming.isLobbed()
            && incoming.getYCoordinate() == zombie.getRow()
            && Math.abs(incoming.getXCoordinate() - zombie.getX()) <= CATCH_RANGE;
  }
  
  private Projectile buildReflectedProjectile(Projectile incoming, Zombie zombie) {
    // تیر با همان قدرت خودش برمی‌گردد (نه با دمیج گازگرفتن زامبی)
    return new Projectile(
            Math.max(incoming.getDamage(), (int) eatingDamage),
            -0.5,
            zombie.getX(),
            zombie.getRow(),
            incoming.getEffect(),
            incoming.isPiercing(),
            false,
            true);
  }

  private void walkOrEat(Zombie zombie, Board board, int currentTick) {
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