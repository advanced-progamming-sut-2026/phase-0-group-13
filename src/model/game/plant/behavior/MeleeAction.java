package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class MeleeAction implements PlantAction {
  private static final double MELEE_RANGE = 1.5;
  private final int actionInterval;
  private final int damage;
  private final int aoeRadius;

  public MeleeAction(int actionInterval, int damage) {
    this(actionInterval, damage, 0);
  }

  public MeleeAction(int actionInterval, int damage, int aoeRadius) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.aoeRadius = Math.max(0, aoeRadius);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) return;

    if (aoeRadius > 0) {
      if (!hasZombieInArea(board, plant)) return;
      board.applyAreaDamageToZombies(plant.getCol(), plant.getRow(), aoeRadius, damage);
      plant.setLastActionTick(currentTick);
      System.out.printf("Plant %s released a sonic wave over a %dx%d area!%n",
              plant.getName(), aoeRadius * 2 + 1, aoeRadius * 2 + 1);
      return;
    }

    Zombie target = findAdjacentZombie(board, plant);
    if (target == null) return;

    target.takeDamage(damage, false);
    plant.setLastActionTick(currentTick);
    System.out.printf("Plant %s hit %s in melee range!%n", plant.getName(), target.getName());
  }

  private boolean hasZombieInArea(Board board, Plant plant) {
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) continue;
      if (Math.abs(zombie.getRow() - plant.getRow()) <= aoeRadius
              && Math.abs(zombie.getX() - plant.getCol()) <= aoeRadius) {
        return true;
      }
    }
    return false;
  }

  private Zombie findAdjacentZombie(Board board, Plant plant) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow()) continue;

      double distance = zombie.getX() - plant.getCol();
      if (distance >= 0 && distance <= MELEE_RANGE && distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
