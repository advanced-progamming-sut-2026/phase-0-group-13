package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class MeleeAction implements PlantAction {
  private static final double MELEE_RANGE = 1.5;
  private final int actionInterval;
  private final int damage;
  private final int aoeRadius;
  private final boolean hitsBehind;
  private boolean warming;

  /** Wasabi Whip: its description says the whip warms the ground around it as well as hitting. */
  public MeleeAction warmingTheGround() {
    this.warming = true;
    return this;
  }

  public MeleeAction(int actionInterval, int damage) {
    this(actionInterval, damage, 0);
  }

  public MeleeAction(int actionInterval, int damage, int aoeRadius) {
    this(actionInterval, damage, aoeRadius, false);
  }

  public MeleeAction(int actionInterval, int damage, int aoeRadius, boolean hitsBehind) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.aoeRadius = Math.max(0, aoeRadius);
    this.hitsBehind = hitsBehind;
  }

  private static final int WARM_RADIUS = 1;

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) return;

    // Independent of whether there is anything to hit: the heat is a property of the plant, so a
    // Wasabi Whip planted next to an ice trail thaws it without waiting for a zombie to turn up.
    if (warming) {
      board.warmTiles(plant.getRow(), plant.getCol(), WARM_RADIUS, plant);
    }

    if (aoeRadius > 0) {
      if (!hasZombieInArea(board, plant)) return;
      board.applyAreaDamageToZombies(plant.getCol(), plant.getRow(), aoeRadius, damage);
      plant.setLastActionTick(currentTick);
      System.out.printf("Plant %s released a sonic wave over a %dx%d area!%n",
              plant.getName(), aoeRadius * 2 + 1, aoeRadius * 2 + 1);
      return;
    }

    Zombie front = findAdjacentZombie(board, plant, true);
    Zombie behind = hitsBehind ? findAdjacentZombie(board, plant, false) : null;
    if (front == null && behind == null) return;

    if (front != null) {
      front.takeDamage(damage, false);
      System.out.printf("Plant %s hit %s in melee range!%n", plant.getName(), front.getName());
    }
    if (behind != null && behind != front) {
      behind.takeDamage(damage, false);
      System.out.printf("Plant %s hit %s behind it!%n", plant.getName(), behind.getName());
    }
    plant.setLastActionTick(currentTick);
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

  /** @param ahead true یعنی خانهٔ جلو (ستون بزرگ‌تر)، false یعنی خانهٔ پشت سر گیاه */
  private Zombie findAdjacentZombie(Board board, Plant plant, boolean ahead) {
    Zombie nearest = null;
    double bestDistance = Double.MAX_VALUE;

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.getRow() != plant.getRow()) continue;

      double offset = zombie.getX() - plant.getCol();
      double distance = ahead ? offset : -offset;
      if (distance >= 0 && distance <= MELEE_RANGE && distance < bestDistance) {
        bestDistance = distance;
        nearest = zombie;
      }
    }
    return nearest;
  }
}
