package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class RaHealAuraZombieAction implements ZombieAction {
  private final int healInterval;
  private final int healAmount;
  private final double range;
  private final double eatingDamage;
  private int lastHealTick = -1;

  // به‌جای رفتار مهاجم خاص، هر healInterval تیک به زامبی‌های زخمی هم‌ردیف تو شعاع range جون برمیگردونه
  public RaHealAuraZombieAction(int healInterval, int healAmount, double range, double eatingDamage) {
    this.healInterval = healInterval;
    this.healAmount = healAmount;
    this.range = range;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastHealTick == -1) {
      lastHealTick = currentTick;
    }
    if (currentTick - lastHealTick >= healInterval) {
      for (Zombie other : board.getZombies()) {
        if (other != zombie
                && !other.isDead()
                && other.getRow() == zombie.getRow()
                && Math.abs(other.getX() - zombie.getX()) <= range) {
          other.heal(healAmount);
        }
      }
      lastHealTick = currentTick;
    }

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