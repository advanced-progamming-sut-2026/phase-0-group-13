package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.Plant;

public class LilyPadSpreadAction implements PlantAction {
  private final PlantFactory factory;
  private final int copies;
  private boolean applied;

  public LilyPadSpreadAction(PlantFactory factory, int copies) {
    this.factory = factory;
    this.copies = copies;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (applied) {
      return;
    }
    applied = true;

    int planted = 0;
    for (int row = 0; row < board.getRows() && planted < copies; row++) {
      for (int col = 0; col < board.getColumns() && planted < copies; col++) {
        if (!board.isWaterAt(row, col) || board.getPlantAt(row, col) != null) {
          continue;
        }
        Plant copy = factory.createPlant(plant.getName(), row, col);
        if (copy != null) {
          board.placePlant(copy);
          planted++;
        }
      }
    }

    if (planted > 0) {
      System.out.printf("%s spread across %d empty water tile(s)!%n", plant.getName(), planted);
    } else {
      System.out.printf("%s found no empty water tile to spread onto.%n", plant.getName());
    }
  }
}
