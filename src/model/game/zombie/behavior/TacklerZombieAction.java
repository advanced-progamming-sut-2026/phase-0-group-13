package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class TacklerZombieAction implements ZombieAction {
  private final boolean selfDestructs;
  /** 1 leaves the pace alone; the All-Star drops to a crawl once it has flattened a plant. */
  private final double slowdownAfterKill;
  private boolean hasTackled;

  public TacklerZombieAction() {
    this(false, 1.0);
  }

  public TacklerZombieAction(boolean selfDestructs) {
    this(selfDestructs, 1.0);
  }

  public TacklerZombieAction(boolean selfDestructs, double slowdownAfterKill) {
    this.selfDestructs = selfDestructs;
    this.slowdownAfterKill = slowdownAfterKill;
  }

  private int lastTackleTick = -1;

  /** The tick it last hit a plant, so the renderer can play the kick/tackle clip. */
  public int getLastTackleTick() {
    return lastTackleTick;
  }

  /** The last plant it flattened, so one charge cannot be spent on the same plant every tick. */
  private Plant lastVictim;

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    // getTopPlantAt, so a shielded plant is hit through its Pumpkin rather than under it.
    Plant targetPlant = board.getTopPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead() && targetPlant != lastVictim) {
      lastVictim = targetPlant;
      lastTackleTick = currentTick;
      targetPlant.takeDamage(10000);
      System.out.printf("%s tackled through %s!%n", zombie.getName(), targetPlant.getName());
      if (selfDestructs) {
        zombie.takeDamage(zombie.getMaxHealth(), true);
        System.out.printf("%s was destroyed along with it.%n", zombie.getName());
        return;
      }
      if (!hasTackled && slowdownAfterKill != 1.0) {
        hasTackled = true;
        zombie.setSpeedMultiplier(slowdownAfterKill);
      }
    }

    zombie.setEating(false);
    zombie.move();
  }
}
