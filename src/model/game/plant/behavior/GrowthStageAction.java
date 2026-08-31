package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public class GrowthStageAction implements PlantAction {
  private final PlantAction[] stages;
  private final int[] stageStartTicks;

  private int plantedTick = -1;
  private int currentStage = 0;

  public GrowthStageAction(PlantAction[] stages, int[] stageStartTicks) {
    this.stages = stages;
    this.stageStartTicks = stageStartTicks;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (plantedTick == -1) {
      plantedTick = currentTick;
      plant.setLastActionTick(currentTick);
    }

    PlantAction activeStage = stages[currentStage];
    if (activeStage != null) {
      activeStage.execute(plant, board, currentTick);
    }

    advanceStageIfDue(plant, currentTick);
  }

  private void advanceStageIfDue(Plant plant, int currentTick) {
    int elapsed = currentTick - plantedTick;
    while (currentStage + 1 < stages.length && elapsed >= stageStartTicks[currentStage + 1]) {
      currentStage++;
      plant.setLastActionTick(currentTick);
      System.out.printf("%s grew to stage %d of %d!%n",
              plant.getName(), currentStage + 1, stages.length);
    }
  }

  public int getCurrentStage() {
    return currentStage + 1;
  }
}
