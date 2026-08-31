package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class WizardZombieAction implements ZombieAction {
  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead() && !targetPlant.isDisabled(currentTick)) {
      targetPlant.applyCurse(zombie);
      System.out.printf(
              "%s turned %s into a harmless sheep!%n", zombie.getName(), targetPlant.getName());
    }

    zombie.setEating(false);
    zombie.move();
  }
}
