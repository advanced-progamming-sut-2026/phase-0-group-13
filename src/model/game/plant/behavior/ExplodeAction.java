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

  public void detonateNow(Plant plant, Board board) {
    System.out.printf(
            "BOOM! %s exploded at (%d, %d)%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);

    board.applyAreaDamageToZombies(plant.getCol(), plant.getRow(), range, damage);
    scatterGrapes(plant, board);
    plant.takeDamage(10000);
  }

  private void scatterGrapes(Plant plant, Board board) {
    if (scatterGrapes <= 0) {
      return;
    }
    // انگورها در جهت‌های مختلف (بالا/پایین/جلو) پرتاب می‌شوند و از دیواره‌ها کمانه می‌کنند
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
