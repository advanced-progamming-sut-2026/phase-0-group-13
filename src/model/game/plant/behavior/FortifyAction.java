package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public class FortifyAction implements PlantAction {
  private final int bonusHealth;
  private boolean applied;

  public FortifyAction(int bonusHealth) {
    this.bonusHealth = bonusHealth;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (applied) {
      return;
    }
    applied = true;
    plant.grantBonusHealth(bonusHealth);
    System.out.printf(
        "%s is fortified! +%d armour health (now %d).%n",
        plant.getName(), bonusHealth, plant.getCurrentHealth());
  }
}
