package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.List;
import model.game.Board;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/** The Ra zombie: drags the sun lying on the lawn to itself, and drops it again when killed. */
public class RaHealAuraZombieAction implements ZombieAction {

  private static final double PULL_SPEED = 0.12;
  private static final double GRAB_RANGE = 0.4;

  private final double range;
  private final double eatingDamage;
  private int stolenSun;
  private boolean droppedLoot;

  public RaHealAuraZombieAction(int unusedInterval, int unusedAmount, double range,
      double eatingDamage) {
    this.range = range;
    this.eatingDamage = eatingDamage;
  }

  /** What it is carrying, for the renderer and for the drop on death. */
  public int getStolenSun() {
    return stolenSun;
  }

  @Override
  public void onDeath(Zombie zombie, Board board) {
    dropStolenSun(zombie, board);
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    stealNearbySun(zombie, board);

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

  private void stealNearbySun(Zombie zombie, Board board) {
    List<Sun> taken = new ArrayList<>();
    for (Sun sun : board.getSuns()) {
      if (sun.isFalling() || Math.abs(sun.getY() - zombie.getRow()) > 1.0
              || Math.abs(sun.getX() - zombie.getX()) > range) {
        continue;
      }
      double gap = zombie.getX() - sun.getX();
      if (Math.abs(gap) <= GRAB_RANGE) {
        stolenSun += sun.getAmount();
        taken.add(sun);
      } else {
        sun.changinCordinate(sun.getX() + Math.signum(gap) * PULL_SPEED, sun.getY());
      }
    }
    for (Sun sun : taken) {
      board.getSuns().remove(sun);
      System.out.printf("%s stole %d sun!%n", zombie.getDisplayName(), sun.getAmount());
    }
  }

  /** The doc gives the stolen sun back to the player when the thief dies. */
  private void dropStolenSun(Zombie zombie, Board board) {
    if (droppedLoot || stolenSun <= 0) {
      return;
    }
    droppedLoot = true;
    board.getGameState().addSun(stolenSun);
    System.out.printf("%s dropped the %d sun it stole.%n", zombie.getDisplayName(), stolenSun);
    stolenSun = 0;
  }
}
