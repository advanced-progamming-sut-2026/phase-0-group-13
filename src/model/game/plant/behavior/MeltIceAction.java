package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public class MeltIceAction implements PlantAction {

  private final int radius;

  public MeltIceAction(int radius) {
    this.radius = Math.max(0, radius);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    int melted = board.warmTiles(plant.getRow(), plant.getCol(), radius, plant);
    System.out.printf("%s melted the ice on %d tile(s).%n", plant.getName(), melted);
    plant.takeDamage(plant.getMaxHealth());
  }
}
