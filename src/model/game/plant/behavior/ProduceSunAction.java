package model.game.plant.behavior;

import model.enums.SunType;
import model.game.Board;
import model.game.Sun;
import model.game.plant.Plant;

public class ProduceSunAction implements PlantAction {
  private static final int DEFAULT_SUN_AMOUNT = 25;

  private final int productionInterval;
  private final int sunAmount;
  private Sun uncollectedSun;

  public ProduceSunAction(int productionInterval) {
    this(productionInterval, DEFAULT_SUN_AMOUNT);
  }

  public ProduceSunAction(int productionInterval, int sunAmount) {
    this.productionInterval = productionInterval;
    this.sunAmount = sunAmount > 0 ? sunAmount : DEFAULT_SUN_AMOUNT;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (uncollectedSun != null && !uncollectedSun.isExpired()) {
      return;
    }

    if (currentTick - plant.getLastActionTick() >= productionInterval) {
      Sun newSun = new Sun(sunAmount, Sun.INFINITE_LIFETIME, SunType.SUNFLOWER);
      newSun.changinCordinate(plant.getCol(), plant.getRow());
      newSun.setGroundedTick(currentTick);
      board.addSun(newSun);
      uncollectedSun = newSun;
      plant.setLastActionTick(currentTick);

      System.out.printf("plant %s produced a sun at (%d, %d)%n",
              plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    }
  }
}
