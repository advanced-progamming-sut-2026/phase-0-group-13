package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class FumeBlastAction implements PlantAction {

  private static final double PUSH_TILES = 1.0;

  private final int damage;
  private final boolean piercing;

  public FumeBlastAction(int damage, boolean piercing) {
    this.damage = damage;
    this.piercing = piercing;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    int hit = 0;
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.isHypnotized() || zombie.getRow() != plant.getRow()) {
        continue;
      }
      if (zombie.getX() < plant.getCol()) {
        continue;
      }
      zombie.takeDamage(damage, false);
      zombie.setEating(false);
      zombie.setX(Math.min(board.getColumns() - 1.0, zombie.getX() + PUSH_TILES));
      hit++;
      if (!piercing) {
        break;
      }
    }
    if (hit > 0) {
      System.out.printf("%s blasted %d zombie(s) back down the lane.%n", plant.getName(), hit);
    }
  }
}
