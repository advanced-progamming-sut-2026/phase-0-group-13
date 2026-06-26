package model.game.plant.behavior;

import java.util.List;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ExplodeAction implements PlantAction {
  private int fuseTime;
  private int damage;
  private int range;

  public ExplodeAction(int fuseTime, int damage, int range) {
    this.fuseTime = fuseTime;
    this.damage = damage;
    this.range = range;
  }

  public ExplodeAction() {
    this(15, 1800, 1);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (plant.getLastActionTick() == 0) {
      plant.setLastActionTick(currentTick);
    }

    if (currentTick - plant.getLastActionTick() >= fuseTime) {
      System.out.printf(
          "BOOM! %s exploded at (%d, %d)%n", plant.getName(), plant.getCol(), plant.getRow());

      List<Zombie> zombies = board.getZombies();

      for (Zombie zombie : zombies) {
        if (zombie.getRow() == plant.getRow()
            && Math.abs(zombie.getX() - plant.getCol()) <= range) {
          zombie.takeDamage(damage, false);
        }
      }

      plant.takeDamage(10000);
    }
  }
}
