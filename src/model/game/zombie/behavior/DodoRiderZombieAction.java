package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class DodoRiderZombieAction implements ZombieAction {
  private boolean isFlying = true;
  private final double eatingDamage;

  // مثل ExplorerZombieAction: تا وقتی سوار دودو (پرنده) هست از رو اولین گیاه پرواز میکنه رد میشه و
  // نابودش میکنه، بعدش پیاده میشه و مثل زامبی عادی گیاه بعدی رو میخوره
  public DodoRiderZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (isFlying) {
      Plant targetPlant = board.getPlantAhead(zombie.getRow(), zombie.getX(), 1.0);
      if (targetPlant != null && !targetPlant.isDead()) {
        targetPlant.takeDamage(10000);
        isFlying = false;
        System.out.printf("%s's Dodo trampled a plant and flew off!%n", zombie.getName());
      }
      zombie.setEating(false);
      zombie.move();
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
