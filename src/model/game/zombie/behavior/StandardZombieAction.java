package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class StandardZombieAction implements ZombieAction {
  private final double eatingDamage;

  public StandardZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (zombie.isHypnotized()) {
      Zombie targetZombie = board.getZombieAt(zombie.getRow(), zombie.getX() + 1);
      if (targetZombie != null && targetZombie != zombie && !targetZombie.isDead()) {
        zombie.setEating(true);
        if (currentTick % 10 == 0) {
          targetZombie.takeDamage((int) eatingDamage, false);
        }
      } else {
        zombie.setEating(false);
        zombie.move();
      }
      return;
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