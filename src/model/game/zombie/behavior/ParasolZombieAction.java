package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ParasolZombieAction implements ZombieAction {
  private final int parasolHp;
  private final double eatingDamage;

  public ParasolZombieAction(int parasolHp, double eatingDamage) {
    this.parasolHp = parasolHp;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    int damageTaken = zombie.getMaxHealth() - zombie.getCurrentHealth();
    zombie.setShieldBlocker(damageTaken < parasolHp);

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
