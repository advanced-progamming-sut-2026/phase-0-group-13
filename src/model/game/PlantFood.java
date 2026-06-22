package model.game;

import model.game.plant.Plant;
import model.game.plant.behavior.PlantAction;

public class PlantFood {

    private int plantFoodStartingDuration;
    private int plantFoodDuration;
    private PlantAction plantFoodAction;

    public PlantFood(int plantFoodStartingDurationDuration, PlantAction plantFoodAction) {
        this.plantFoodStartingDuration =plantFoodStartingDurationDuration;
        this.plantFoodDuration = 0;
        this.plantFoodAction = plantFoodAction;
    }

    public void execute(Plant plant, Board board, int currentTick) {
        if(plantFoodDuration == 0) return;
        plantFoodDuration --;
        plantFoodAction.execute(plant, board, currentTick);
    }

    public boolean canExecute() {
        return plantFoodDuration > 0;
    }

    public void activate() {
        this.plantFoodDuration = plantFoodStartingDuration;
    }
}
