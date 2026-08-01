package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ShootForwardAction implements PlantAction {
  private static final int[][] FORWARD_ONLY = {{1, 0}};

  private final int actionInterval;
  private final int damage;
  private final Projectile.ProjectileEffect effect;
  private final boolean piercing;
  private final int laneSpread;
  private int[][] directions = FORWARD_ONLY;

  public ShootForwardAction(int actionInterval, int damage, Projectile.ProjectileEffect effect,
                            boolean piercing, int laneSpread) {
    this.actionInterval = actionInterval;
    this.damage = damage;
    this.effect = effect == null ? Projectile.ProjectileEffect.NORMAL : effect;
    this.piercing = piercing;
    this.laneSpread = Math.max(0, laneSpread);
  }

  public ShootForwardAction(int actionInterval, int damage, Projectile.ProjectileEffect effect, boolean piercing) {
    this(actionInterval, damage, effect, piercing, 0);
  }

  public ShootForwardAction(int actionInterval, int damage, Projectile.ProjectileEffect effect) {
    this(actionInterval, damage, effect, false, 0);
  }

  public ShootForwardAction(int actionInterval, int damage) {
    this(actionInterval, damage, Projectile.ProjectileEffect.NORMAL, false, 0);
  }

  public void setDirections(int[][] directions) {
    this.directions = directions == null || directions.length == 0 ? FORWARD_ONLY : directions;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }
    if (!hasTargetInAnyLane(plant, board)) {
      return;
    }

    for (int row = plant.getRow() - laneSpread; row <= plant.getRow() + laneSpread; row++) {
      if (!isValidRow(board, row)) {
        continue;
      }
      for (int[] direction : directions) {
        for (int shot = Math.max(1, plant.getStackCount()); shot > 0; shot--) {
          Projectile projectile =
                  new Projectile(damage, 0.5, plant.getCol(), row, effect, piercing, false, false);
          projectile.setDirection(direction[0], direction[1]);
          board.addProjectile(projectile);
        }
      }
      System.out.printf("Plant %s fired a %s projectile at row %d!%n",
              plant.getName(), effect, row + 1);
    }
    plant.setLastActionTick(currentTick);
  }

  private boolean hasTargetInAnyLane(Plant plant, Board board) {
    for (int row = plant.getRow() - laneSpread; row <= plant.getRow() + laneSpread; row++) {
      if (isValidRow(board, row) && board.hasZombieInRow(row, plant.getCol())) {
        return true;
      }
    }
    if (directions.length > 1) {
      for (Zombie zombie : board.getZombies()) {
        if (!zombie.isDead()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isValidRow(Board board, int row) {
    return row >= 0 && row < board.getRows();
  }
}
