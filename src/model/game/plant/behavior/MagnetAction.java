package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;

public class MagnetAction implements PlantAction {

  private final int actionInterval;
  private final int maxTargets;
  private final double range;

  public MagnetAction(int actionInterval, int maxTargets, double range) {
    this.actionInterval = Math.max(1, actionInterval);
    this.maxTargets = Math.max(1, maxTargets);
    this.range = range;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    int stripped = 0;
    for (Zombie zombie : board.getZombies()) {
      if (stripped >= maxTargets) {
        break;
      }
      if (zombie.isDead() || zombie.isHypnotized() || !inRange(plant, zombie)) {
        continue;
      }
      if (stripMetal(plant, zombie)) {
        stripped++;
      }
    }

    if (stripped > 0) {
      plant.setLastActionTick(currentTick);
    }
  }

  /**
   * A magnet reaches out around itself, not only along its own lane and not backwards forever.
   *
   * <p>The old test was {@code zombie.getX() - plant.getCol() <= range} in the plant's own row,
   * which let it grab a zombie any distance behind it and never one a lane over.
   */
  private boolean inRange(Plant plant, Zombie zombie) {
    double lanes = Math.abs(zombie.getRow() - plant.getRow());
    double tiles = Math.abs(zombie.getX() - plant.getCol());
    return lanes <= 1 && Math.hypot(tiles, lanes) <= range;
  }

  /**
   * Takes every piece of metal off one zombie, not the first piece.
   *
   * <p>The doc says Magnet-shroom "disarms zombies by pulling metal objects (buckets/helmets) off
   * their heads" -- plural, and a Knight wears two. Stopping at the first left armour behind and
   * the zombie still looked and behaved armoured.
   */
  private boolean stripMetal(Plant plant, Zombie zombie) {
    boolean pulled = false;
    for (Armor armor : zombie.getArmors()) {
      if (armor.isMetallic() && !armor.isDestroyed()) {
        armor.takeDamage(armor.getCurrentHealth());
        System.out.printf("%s pulled the %s off %s!%n",
                plant.getName(), armor.getType(), zombie.getName());
        pulled = true;
      }
    }
    return pulled;
  }
}
