package model.game.zombie.behavior;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

public class GargantuarAction implements ZombieAction {
  private static final int THROW_TICKS = 8;

  private final int maxHealth;
  private boolean hasThrownImp;
  private int throwTick = -1;
  private int smashTick = -1;

  public GargantuarAction(int maxHealth) {
    this.maxHealth = maxHealth;
    this.hasThrownImp = false;
  }

  public int getThrowTick() {
    return throwTick;
  }

  /**
   * The tick the pole last came down, or -1 before the first one.
   *
   * <p>A Gargantuar does not bite: it stops at a plant and smashes it, and its rig has a
   * smash_left for exactly that. The model still reports it as eating, because that is what the
   * Hypno-shroom and the terminal board read to know a zombie is occupied; this is only so the
   * renderer can tell a landed smash from the wind-up between them.
   */
  public int getSmashTick() {
    return smashTick;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!hasThrownImp && zombie.getCurrentHealth() <= maxHealth / 2) {
      ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);

      Zombie imp =
              factory.createZombie("ZombieEgyptImpDefault", zombie.getRow(), zombie.getX() - 2.0);

      if (imp != null) {
        imp.markThrownFrom(zombie.getX(), THROW_TICKS);
        board.spawnZombie(imp);
        System.out.println("Gargantuar threw an Imp!");
      }
      hasThrownImp = true;
      throwTick = currentTick;
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 15 == 0) {
        targetPlant.takeDamage(10000);
        smashTick = currentTick;
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}
