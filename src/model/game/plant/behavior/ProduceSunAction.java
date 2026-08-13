package model.game.plant.behavior;

import model.enums.SunType;
import model.game.Board;
import model.game.Sun;
import model.game.plant.Plant;

public class ProduceSunAction implements PlantAction {
  private static final int DEFAULT_SUN_AMOUNT = 25;

  private final int productionInterval;
  private final int sunAmount;
  private final boolean singleUse;
  private Sun uncollectedSun;

  public ProduceSunAction(int productionInterval) {
    this(productionInterval, DEFAULT_SUN_AMOUNT);
  }

  public ProduceSunAction(int productionInterval, int sunAmount) {
    this(productionInterval, sunAmount, false);
  }

  /**
   * singleUse برای گیاهانی مثل Gold Bloom که در plants.json «یک‌بار خورشید می‌دهد و بعد ناپدید
   * می‌شود» ثبت شده‌اند؛ بدون این، همان گیاه بی‌نهایت بار خورشید تولید می‌کند.
   */
  public ProduceSunAction(int productionInterval, int sunAmount, boolean singleUse) {
    this.productionInterval = productionInterval;
    this.sunAmount = sunAmount > 0 ? sunAmount : DEFAULT_SUN_AMOUNT;
    this.singleUse = singleUse;
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

      if (singleUse) {
        plant.takeDamage(plant.getMaxHealth());
        System.out.printf("%s vanished after its one-off burst.%n", plant.getName());
      }
    }
  }
}
