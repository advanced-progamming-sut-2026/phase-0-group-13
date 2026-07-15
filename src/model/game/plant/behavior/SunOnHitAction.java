package model.game.plant.behavior;

import model.enums.SunType;
import model.game.Board;
import model.game.Sun;
import model.game.plant.Plant;

public class SunOnHitAction implements PlantAction {
  private final int sunPerHit;
  private int lastObservedHealth = -1;

  // مثل ReflectDamageAction با تشخیص افت جون نسبت به تیک قبل کار میکنه؛ به‌جای رفلکت دمیج، خورشید
  // تولید میکنه (Sun Bean)
  public SunOnHitAction(int sunPerHit) {
    this.sunPerHit = sunPerHit;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (lastObservedHealth == -1) {
      lastObservedHealth = plant.getCurrentHealth();
      return;
    }

    int damageTaken = lastObservedHealth - plant.getCurrentHealth();
    if (damageTaken > 0) {
      Sun sun = new Sun(sunPerHit, 150, SunType.SUNFLOWER);
      sun.changinCordinate(plant.getCol(), plant.getRow());
      board.addSun(sun);
    }

    lastObservedHealth = plant.getCurrentHealth();
  }
}
