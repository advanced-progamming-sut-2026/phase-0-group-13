package model.game.zombie.behavior;

import model.enums.ZombieType;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;


public class DummyZombieAction implements ZombieAction {
  private final ZombieType type;
  private final double eatDamage;
  private boolean warned = false;

  public DummyZombieAction(ZombieType type, double eatDamage) {
    this.type = type;
    this.eatDamage = eatDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!warned) {
      System.out.printf(
              "[NOT IMPLEMENTED] %s (%s) falls back to generic walk/eat behavior%n",
              zombie.getName(), type);
      warned = true;
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatDamage);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}