package model.game.plant.behavior;

import model.enums.SunType;
import model.game.Board;
import model.game.Sun;
import model.game.plant.Plant;

public class ProduceSunAction implements PlantAction {
    private int productionInterval;

    public ProduceSunAction(int productionInterval) {
        this.productionInterval = productionInterval;
    }

    @Override
    public void execute(Plant plant, Board board, int currentTick) {
        if (currentTick - plant.getLastActionTick() >= productionInterval) {

            Sun newSun = new Sun(25, 150, SunType.SUNFLOWER);
            newSun.changinCordinate(plant.getCol(), plant.getRow());

            board.addSun(newSun);
            plant.setLastActionTick(currentTick);

            System.out.printf("Plant %s produced a sun at (%d, %d).%n", plant.getName(), plant.getCol(), plant.getRow());
        }
    }
}