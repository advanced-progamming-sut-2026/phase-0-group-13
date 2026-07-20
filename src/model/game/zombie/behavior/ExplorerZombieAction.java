package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ExplorerZombieAction implements ZombieAction {
  private boolean isTorchLit;

  public ExplorerZombieAction() {
    this.isTorchLit = true;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (isTorchLit) {
      // چک کردن گیاه در فاصله یک خانه‌ای جلوتر
      Plant targetPlant = board.getPlantAhead(zombie.getRow(), zombie.getX(), 1.0);

      if (targetPlant != null && !targetPlant.isDead()) {
        targetPlant.takeDamage(1000);
      }

      zombie.setEating(false);
      zombie.move();

    } else {
      Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
      if (targetPlant != null && !targetPlant.isDead()) {
        zombie.setEating(true);
        if (currentTick % 10 == 0) {
          targetPlant.takeDamage(10);
        }
      } else {
        zombie.setEating(false);
        zombie.move();
      }
    }
  }

  public void extinguishTorch() {
    this.isTorchLit = false;
  }

  public void lightTorch() {
    this.isTorchLit = true;
  }

  public boolean isTorchLit() {
    return isTorchLit;
  }
}