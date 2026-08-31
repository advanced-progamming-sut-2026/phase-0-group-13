package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ExplodeAction implements PlantAction {
  private final int fuseTime;
  private final int damage;
  private final int range;
  private final boolean requiresContact;
  private boolean isInitialized;
  private boolean armedMessageShown;
  private int scatterGrapes;
  private int scatterLifeTicks;
  private int scatterDamage;
  private boolean laneOnly;

  public ExplodeAction(int fuseTime, int damage, int range, boolean requiresContact) {
    this.fuseTime = fuseTime;
    this.damage = damage;
    this.range = range;
    this.requiresContact = requiresContact;
    this.isInitialized = false;
  }

  public ExplodeAction(int fuseTime, int damage, int range) {
    this(fuseTime, damage, range, fuseTime > 0);
  }

  public ExplodeAction() {
    this(15, 1800, 1);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (!isInitialized) {
      plant.setLastActionTick(currentTick);
      isInitialized = true;
    }

    if (currentTick - plant.getLastActionTick() < fuseTime) {
      return;
    }

    if (!requiresContact) {
      detonateNow(plant, board);
      return;
    }

    if (!armedMessageShown) {
      armedMessageShown = true;
      System.out.printf(
              "%s is armed at (%d, %d) and waiting for a zombie.%n",
              plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    }
    if (hasZombieInRange(plant, board)) {
      detonateNow(plant, board);
    }
  }

  private boolean hasZombieInRange(Plant plant, Board board) {
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()
              && zombie.getRow() == plant.getRow()
              && Math.abs(zombie.getX() - plant.getCol()) <= range) {
        return true;
      }
    }
    return false;
  }

  /**
   * بعد از انفجار، چند انگورِ کمانه‌کننده پخش می‌کند (مخصوص Grapeshot).
   *
   * @param count تعداد انگورها، {@code lifeTicks} عمرشان و {@code grapeDamage} آسیب هرکدام
   */
  public ExplodeAction withScatteringGrapes(int count, int lifeTicks, int grapeDamage) {
    this.scatterGrapes = count;
    this.scatterLifeTicks = lifeTicks;
    this.scatterDamage = grapeDamage;
    return this;
  }

  public ExplodeAction asLaneWide() {
    this.laneOnly = true;
    return this;
  }

  public ExplodeAction leavingCrater() {
    this.leavesCrater = true;
    return this;
  }

  private boolean leavesCrater;

  public void detonateNow(Plant plant, Board board) {
    System.out.printf(
            "BOOM! %s exploded at (%d, %d)%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);

    if (laneOnly) {
      scorchLane(plant, board);
    } else {
      board.applyAreaDamageToZombies(plant.getCol(), plant.getRow(), range, damage);
    }
    scatterGrapes(plant, board);
    if (leavesCrater) {
      board.placeTileEffect(plant.getRow(), plant.getCol(),
              new model.game.TileEffects.CraterEffect());
      System.out.printf("%s left an unplantable crater at (%d, %d).%n",
              plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    }
    plant.takeDamage(10000);
  }

  private void scorchLane(Plant plant, Board board) {
    int burned = 0;
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombie.getRow() == plant.getRow()) {
        zombie.takeDamage(damage, false);
        burned++;
      }
    }
    for (int col = 0; col < board.getColumns(); col++) {
      model.game.Tile tile = board.getTile(plant.getRow(), col);
      if (tile != null && tile.getEffect() instanceof model.game.TileEffects.IceTrailEffect) {
        tile.setEffect(null);
      }
      Plant frozen = board.getPlantAt(plant.getRow(), col);
      if (frozen != null && frozen != plant) {
        frozen.meltIce();
      }
    }
    System.out.printf("%s torched %d zombie(s) across row %d.%n",
            plant.getName(), burned, plant.getRow() + 1);
  }

  private void scatterGrapes(Plant plant, Board board) {
    if (scatterGrapes <= 0) {
      return;
    }
    int[][] directions = {{1, 0}, {1, -1}, {1, 1}, {-1, -1}, {-1, 1}, {-1, 0}};
    for (int i = 0; i < scatterGrapes; i++) {
      int[] dir = directions[i % directions.length];
      Projectile grape = new Projectile(scatterDamage, 0.5, plant.getCol(), plant.getRow(),
              Projectile.ProjectileEffect.NORMAL, true, false, false);
      grape.setDirection(dir[0], dir[1]);
      grape.makeBouncing(scatterLifeTicks);
      board.addProjectile(grape);
    }
    System.out.printf("%s scattered %d ricocheting grapes!%n", plant.getName(), scatterGrapes);
  }
}
