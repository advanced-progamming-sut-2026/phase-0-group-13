package model.game.zombie.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ZombotanyPeashooterAction implements ZombieAction {
  private int shootInterval;
  private int lastShootTick;
  private double eatingDamage;

  public ZombotanyPeashooterAction(int shootInterval, double eatingDamage) {
    this.shootInterval = shootInterval;
    this.eatingDamage = eatingDamage;
    this.lastShootTick = 0;
  }

  public ZombotanyPeashooterAction() {
    this(150, 10.0);
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (currentTick - lastShootTick >= shootInterval) {
      Projectile p = new Projectile(20, -0.5, zombie.getX(), zombie.getRow(), false, true);
      board.addProjectile(p);

      lastShootTick = currentTick;
      System.out.printf("Zombotany Peashooter Zombie fired at row %d!%n", zombie.getRow());
    }

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
