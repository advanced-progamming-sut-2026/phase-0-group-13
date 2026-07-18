package model.game.zombie.behavior;

import java.util.Iterator;
import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;


public class JesterZombieAction implements ZombieAction {
  private static final double CATCH_RANGE = 0.6;

  private final double eatingDamage;

  public JesterZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    reflectIncomingProjectiles(zombie, board);
    walkOrEat(zombie, board, currentTick);
  }

  private void reflectIncomingProjectiles(Zombie zombie, Board board) {
    Iterator<Projectile> iterator = board.getProjectiles().iterator();
    while (iterator.hasNext()) {
      Projectile incoming = iterator.next();
      if (!canReflect(incoming, zombie)) {
        continue;
      }
      iterator.remove();
      board.addProjectile(buildReflectedProjectile(incoming, zombie));
      System.out.printf("%s juggled a projectile back at the plants!%n", zombie.getName());
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
    return new Projectile(
            (int) eatingDamage,
            -0.5,
            zombie.getX(),
            zombie.getRow(),
            incoming.getEffect(),
            incoming.isPiercing(),
            false,
            true);
  }

  private void walkOrEat(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
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