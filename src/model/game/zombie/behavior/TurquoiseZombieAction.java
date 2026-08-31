package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class TurquoiseZombieAction implements ZombieAction {
  private static final int SUN_PER_STEAL = 25;
  private static final int LASER_RANGE = 4;

  private final int chargeTicks;
  private final int stealIntervalTicks;

  private int chargeStartTick = -1;
  private int lastStealTick = -1;
  private int stolenSun;

  public TurquoiseZombieAction(int chargeTicks, int stealIntervalTicks) {
    this.chargeTicks = Math.max(1, chargeTicks);
    this.stealIntervalTicks = Math.max(1, stealIntervalTicks);
  }

  /** The tick of its last special action, so the renderer can hold that pose. */
  public int getLastStealTick() {
    return lastStealTick;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant victim = board.getPlantAhead(zombie.getRow(), zombie.getX(), LASER_RANGE);

    if (victim == null || victim.isDead()) {
      chargeStartTick = -1;
      zombie.setEating(false);
      zombie.move();
      return;
    }

    zombie.setEating(false);
    if (chargeStartTick == -1) {
      chargeStartTick = currentTick;
      lastStealTick = currentTick;
      System.out.printf("%s spotted %s and started siphoning your sun!%n",
              zombie.getName(), victim.getName());
    }

    stealSun(zombie, board, currentTick);

    if (currentTick - chargeStartTick >= chargeTicks) {
      fireLaser(zombie, board);
      chargeStartTick = -1;
    }
  }

  private void stealSun(Zombie zombie, Board board, int currentTick) {
    if (currentTick - lastStealTick < stealIntervalTicks) {
      return;
    }
    lastStealTick = currentTick;

    if (board.getGameState().getCurrentSun() < SUN_PER_STEAL) {
      return;
    }
    if (board.getGameState().deductSun(SUN_PER_STEAL)) {
      stolenSun += SUN_PER_STEAL;
      System.out.printf("%s stole %d sun (%d stolen so far).%n",
              zombie.getName(), SUN_PER_STEAL, stolenSun);
    }
  }

  private void fireLaser(Zombie zombie, Board board) {
    System.out.printf("%s fired a laser down row %d!%n", zombie.getName(), zombie.getRow() + 1);
    int nearestCol = (int) Math.floor(zombie.getX());
    for (int col = nearestCol; col > nearestCol - LASER_RANGE; col--) {
      if (col < 0 || col >= board.getColumns()) {
        continue;
      }
      Plant plant = board.getPlantAt(zombie.getRow(), col);
      if (plant != null && !plant.isDead()) {
        plant.takeDamage(10000);
        System.out.printf("  %s was vaporised at (%d, %d).%n",
                plant.getName(), col + 1, zombie.getRow() + 1);
      }
    }
  }

  @Override
  public void onDeath(Zombie zombie, Board board) {
    if (stolenSun <= 0) {
      return;
    }
    int dropped = stolenSun / 2;
    stolenSun = 0;
    board.getGameState().addSun(dropped);
    System.out.printf("%s dropped %d of the sun it had stolen!%n", zombie.getName(), dropped);
  }
}
