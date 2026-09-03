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
  private int ricochetLifeTicks;
  private int pierceLimit;
  private int volleySize = 1;

  /**
   * How far apart the shots of one volley are drawn, as a fraction of a lane.
   *
   * <p>Small on purpose: enough that four peas leaving a Mega Gatling on the same tick read as
   * four, not enough to make any of them look like it is in the next lane.
   */
  private static final double MUZZLE_SPREAD = 0.13;

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

  public void setRicochet(int lifeTicks) {
    this.ricochetLifeTicks = Math.max(0, lifeTicks);
  }

  public void setPierceLimit(int pierceLimit) {
    this.pierceLimit = Math.max(0, pierceLimit);
  }

  /** Shots per direction per volley: Repeater's 2, Mega Gatling Pea's 4. */
  public void setVolleySize(int volleySize) {
    this.volleySize = Math.max(1, volleySize);
  }

  public int getVolleySize() {
    return volleySize;
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
        // A Pea Pod fires one pea per head it is wearing; everything else fires its volley.
        int shots = Math.max(volleySize, plant.getStackCount());
        for (int shot = 0; shot < shots; shot++) {
          Projectile projectile =
                  new Projectile(damage, 0.5, plant.getCol(), row, effect, piercing, false, false);
          projectile.firedBy(plant.getName());
          projectile.withPierceLimit(pierceLimit);
          projectile.fromMuzzle(muzzleOffset(shot, shots));
          projectile.launchedFromRow(plant.getRow());
          projectile.setDirection(direction[0], direction[1]);
          if (ricochetLifeTicks > 0) {
            projectile.setDirection(direction[0], direction[1] != 0 ? direction[1] : 1);
            projectile.makeBouncing(ricochetLifeTicks);
          }
          board.addProjectile(projectile);
        }
      }
      System.out.printf("Plant %s fired a %s projectile at row %d!%n",
              plant.getName(), effect, row + 1);
    }
    plant.setLastActionTick(currentTick);
  }

  /** Stacks the shots of one volley up the plant, centred, so a single shot is still on the axis. */
  private static double muzzleOffset(int shot, int shots) {
    return shots <= 1 ? 0 : (shot - (shots - 1) / 2.0) * MUZZLE_SPREAD;
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
