package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public class MintAction implements PlantAction {
  private final int durationTicks;
  private int plantedTick = -1;
  private boolean boostApplied;

  public MintAction(int durationTicks) {
    this.durationTicks = Math.max(1, durationTicks);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (plantedTick == -1) {
      plantedTick = currentTick;
    }

    if (!boostApplied) {
      boostApplied = true;
      applyFamilyBoost(plant, board);
    }

    if (currentTick - plantedTick >= durationTicks) {
      System.out.printf("%s wore off and vanished.%n", plant.getName());
      plant.takeDamage(plant.getMaxHealth());
    }
  }

  private void applyFamilyBoost(Plant plant, Board board) {
    int boosted = 0;
    for (Plant other : board.getPlants()) {
      if (other != plant && !other.isDead() && other.getCategory() == plant.getCategory()) {
        other.applyPlantFood();
        boosted++;
      }
    }
    System.out.printf("%s energised %d %s plant(s) across the lawn!%n",
            plant.getName(), boosted, plant.getCategory());
  }
}
