package model.game.zombie.behavior;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

public class GargantuarAction implements ZombieAction {
  private int maxHealth;
  private boolean hasThrownImp;

  public GargantuarAction(int maxHealth) {
    this.maxHealth = maxHealth;
    this.hasThrownImp = false;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!hasThrownImp && zombie.getCurrentHealth() <= maxHealth / 2) {
      // ایمپ و پرت میکنه گولاخه
      ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);

      // "imp" باگ داشت: ZombieRepository.find دقیقا با alias مطابقت میخواد، و همچین اسمی وجود
      // نداره (فقط "ZombieEgyptImpDefault" هست) - قبلا همیشه silently fail میشد
      Zombie imp =
          factory.createZombie("ZombieEgyptImpDefault", zombie.getRow(), zombie.getX() - 2.0);

      if (imp != null) {
        board.spawnZombie(imp);
        System.out.println("Gargantuar threw an Imp!");
      }
      hasThrownImp = true;
    }

    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 15 == 0) {
        targetPlant.takeDamage(10000); // بمیره
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}
