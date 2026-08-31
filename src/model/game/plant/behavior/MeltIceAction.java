package model.game.plant.behavior;

import model.game.Board;
import model.game.TileEffects.IceTrailEffect;
import model.game.Tile;
import model.game.plant.Plant;

public class MeltIceAction implements PlantAction {

  private final int radius;

  public MeltIceAction(int radius) {
    this.radius = Math.max(0, radius);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    int melted = 0;
    for (int row = plant.getRow() - radius; row <= plant.getRow() + radius; row++) {
      for (int col = plant.getCol() - radius; col <= plant.getCol() + radius; col++) {
        if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getColumns()) {
          continue;
        }
        melted += meltAt(board, row, col, plant);
      }
    }

    System.out.printf("%s melted the ice on %d tile(s).%n", plant.getName(), melted);
    plant.takeDamage(plant.getMaxHealth());
  }

  private int meltAt(Board board, int row, int col, Plant self) {
    int melted = 0;
    Tile tile = board.getTile(row, col);
    if (tile != null && tile.getEffect() instanceof IceTrailEffect ice && ice.isActive()) {
      ice.remove();
      tile.setEffect(null);
      melted++;
    }
    Plant frozen = board.getPlantAt(row, col);
    if (frozen != null && frozen != self && frozen.getIceHealth() > 0) {
      frozen.meltIce();
      melted++;
    }
    return melted;
  }
}
