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

  public GargantuarAction(int maxHealth) {
    this.maxHealth = maxHealth;
    this.hasThrownImp = false;
  }

  public int getThrowTick() {
    return throwTick;
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
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}
