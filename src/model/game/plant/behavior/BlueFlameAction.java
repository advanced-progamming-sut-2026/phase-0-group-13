package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

/**
 * اثر غذای گیاه برای Torchwood: شعله را آبی می‌کند تا تیرهای عبوری به جای ۲ برابر، ۳ برابر
 * آسیب بزنند.
 */
public class BlueFlameAction implements PlantAction {
  private boolean applied;

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (applied) {
      return;
    }
    applied = true;
    plant.setBlueFlame(true);
    System.out.printf(
        "%s burns with a blue flame! Passing shots now deal triple damage.%n", plant.getName());
  }
}
