package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class TacklerZombieAction implements ZombieAction {
  private final boolean selfDestructs;

  public TacklerZombieAction() {
    this(false);
  }

  public TacklerZombieAction(boolean selfDestructs) {
    this.selfDestructs = selfDestructs;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead()) {
      targetPlant.takeDamage(10000);
      System.out.printf("%s tackled through %s!%n", zombie.getName(), targetPlant.getName());
      if (selfDestructs) {
        zombie.takeDamage(zombie.getMaxHealth(), true);
        System.out.printf("%s was destroyed along with it.%n", zombie.getName());
        return;
      }
    }

    zombie.setEating(false);
    zombie.move();
  }
}
