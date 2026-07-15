package model.game.zombie.behavior;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

public class TombRaiserZombieAction implements ZombieAction {
  private final int raiseInterval;
  private final double eatingDamage;
  private int lastRaiseTick = -1;

  // مثل GargantuarAction که یه بار Imp پرت میکنه، ولی این هر raiseInterval تیک یه Imp جدید از پشت سر
  // خودش زنده میکنه (تا وقتی زنده‌س)
  public TombRaiserZombieAction(int raiseInterval, double eatingDamage) {
    this.raiseInterval = raiseInterval;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastRaiseTick == -1) {
      lastRaiseTick = currentTick;
    }
    if (currentTick - lastRaiseTick >= raiseInterval) {
      ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
      Zombie imp =
          factory.createZombie("ZombieEgyptImpDefault", zombie.getRow(), zombie.getX() + 1.0);
      if (imp != null) {
        board.spawnZombie(imp);
        System.out.printf("%s raised an Imp from the grave!%n", zombie.getName());
      }
      lastRaiseTick = currentTick;
    }

    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
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
}
