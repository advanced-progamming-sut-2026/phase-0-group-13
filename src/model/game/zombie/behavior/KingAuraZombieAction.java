package model.game.zombie.behavior;

import model.enums.ArmorType;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;

public class KingAuraZombieAction implements ZombieAction {

  /** Same layers the Knight sheet carries, so a promoted zombie matches a spawned one. */
  private static final int KNIGHT_ARMOR_HEALTH = 1600;

  private final double range;
  private final double eatingDamage;
  private final int promoteInterval;
  private int lastPromoteTick = -1;

  public KingAuraZombieAction(double unusedSpeedMultiplier, double range, double eatingDamage) {
    this(range, eatingDamage, 100);
  }

  public KingAuraZombieAction(double range, double eatingDamage, int promoteInterval) {
    this.range = range;
    this.eatingDamage = eatingDamage;
    this.promoteInterval = Math.max(1, promoteInterval);
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastPromoteTick == -1) {
      lastPromoteTick = currentTick;
    }
    if (currentTick - lastPromoteTick >= promoteInterval) {
      lastPromoteTick = currentTick;
      promoteOne(zombie, board);
    }

    // The doc keeps the King in the column he arrived in; he only ever eats what reaches him.
    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
    } else {
      zombie.setEating(false);
    }
  }

  private void promoteOne(Zombie king, Board board) {
    for (Zombie other : board.getZombies()) {
      if (other == king || other.isDead() || other.isBoss()
              || !other.getArmors().isEmpty()
              || other.getRow() != king.getRow()
              || Math.abs(other.getX() - king.getX()) > range) {
        continue;
      }
      other.addArmor(new Armor(other.getName() + " Helmet", KNIGHT_ARMOR_HEALTH, true,
              ArmorType.HELMET));
      other.addArmor(new Armor(other.getName() + " Shoulder", KNIGHT_ARMOR_HEALTH, false,
              ArmorType.SHOULDER_ARMOR));
      System.out.printf("%s knighted %s!%n", king.getDisplayName(), other.getDisplayName());
      return;
    }
  }
}
