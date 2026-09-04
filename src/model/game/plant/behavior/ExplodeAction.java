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
  private boolean detonated;
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

  /**
   * True once the fuse has burnt down and this plant is buried and waiting for something to step
   * on it. Only ever true for the contact plants -- a mine, a squash, a tangle kelp -- because the
   * timed ones detonate the moment their fuse ends rather than waiting. For the renderer's armed
   * pose, which is a different rig clip from a plant standing about.
   */
  public boolean isArmed() {
    return armedMessageShown;
  }

  /** True once this plant has actually gone off, as opposed to sitting armed and waiting. */
  public boolean hasDetonated() {
    return detonated;
  }

  /** True for a trap that waits to be stepped on rather than burning a fuse of its own. */
  public boolean isContactTrap() {
    return requiresContact;
  }

  /** How long this plant burns before it goes off, in ticks; zero for one that never waits. */
  public int getFuseTicks() {
    return fuseTime;
  }

  /**
   * True while a thrown explosive is counting down and has not gone off yet.
   *
   * <p>These plants have a single hit point, so a zombie already standing on the tile can bite one
   * apart during the fuse. It still goes off when that happens -- see
   * {@code Board.triggerDeathExplosions} -- so the blast is never simply lost.
   */
  public boolean isFuseLit() {
    return fuseTime > 0 && isInitialized && !requiresContact && !detonated;
  }

  /**
   * How far through its fuse a timed explosive is, from 0 the tick it was planted to 1 the tick it
   * detonates, or -1 when there is no fuse running.
   *
   * <p>The renderer plays the plant's explode clip against this rather than against the wall
   * clock, so however long that clip happens to be it finishes exactly on the blast instead of
   * running out early and dropping the plant back to an idle pose while the fuse is still burning.
   */
  public double fuseProgress(Plant plant, int currentTick) {
    if (fuseTime <= 0 || !isInitialized || requiresContact || detonated) {
      return -1;
    }
    double burnt = (currentTick - plant.getLastActionTick()) / (double) fuseTime;
    return Math.max(0.0, Math.min(1.0, burnt));
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
    detonated = true;
    System.out.printf(
            "BOOM! %s exploded at (%d, %d)%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);

    boolean fiery = plant.getTags().contains(model.enums.PlantTag.FIRE);
    if (laneOnly) {
      board.recordBlast(plant.getRow(), plant.getCol(), board.getColumns(), true);
      scorchLane(plant, board);
    } else {
      board.recordBlast(plant.getRow(), plant.getCol(), Math.max(1, range), fiery);
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
        zombie.takeBlastDamage(damage);
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
      grape.firedBy(plant.getName());
      grape.setDirection(dir[0], dir[1]);
      grape.makeBouncing(scatterLifeTicks);
      board.addProjectile(grape);
    }
    System.out.printf("%s scattered %d ricocheting grapes!%n", plant.getName(), scatterGrapes);
  }
}
