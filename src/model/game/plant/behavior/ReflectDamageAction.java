package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ReflectDamageAction implements PlantAction {
  private int lastObservedHealth = -1;

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (lastObservedHealth == -1) {
      lastObservedHealth = plant.getCurrentHealth();
      return;
    }

    int damageTaken = lastObservedHealth - plant.getCurrentHealth();
    if (damageTaken > 0) {
      Zombie attacker = board.getZombieAt(plant.getRow(), plant.getCol());
      if (attacker != null) {
        attacker.takeDamage(damageTaken, false);
      }
    }
    lastObservedHealth = plant.getCurrentHealth();
  }
}