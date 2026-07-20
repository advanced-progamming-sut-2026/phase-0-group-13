package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class KingAuraZombieAction implements ZombieAction {
  private final double buffSpeedMultiplier;
  private final double range;
  private final double eatingDamage;

  // هر تیک به همه‌ی زامبی‌های هم‌ردیفِ تو شعاع range سرعت بیشتر میده (از همون setSpeedMultiplier که
  // برای Newspaper هم استفاده شده)؛ خودش سرعتش عادیه
  public KingAuraZombieAction(double buffSpeedMultiplier, double range, double eatingDamage) {
    this.buffSpeedMultiplier = buffSpeedMultiplier;
    this.range = range;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    for (Zombie other : board.getZombies()) {
      if (other != zombie
              && !other.isDead()
              && other.getRow() == zombie.getRow()
              && Math.abs(other.getX() - zombie.getX()) <= range) {
        other.setSpeedMultiplier(buffSpeedMultiplier);
      }
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