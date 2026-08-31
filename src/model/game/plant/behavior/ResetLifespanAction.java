package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public class ResetLifespanAction implements PlantAction {

  private final PlantAction burst;

  public ResetLifespanAction(PlantAction burst) {
    this.burst = burst;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (burst != null) {
      burst.execute(plant, board, currentTick);
    }

    int refreshed = 0;
    for (Plant other : board.getPlants()) {
      if (!other.isDead() && other.getLifespanTicks() > 0
              && other.getName().equalsIgnoreCase(plant.getName())) {
        other.resetLifespan(currentTick);
        refreshed++;
      }
    }
    if (refreshed > 0) {
      System.out.printf("%s refreshed the lifespan of %d %s(s).%n",
              plant.getName(), refreshed, plant.getName());
    }
  }
}
