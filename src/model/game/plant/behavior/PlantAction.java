package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public interface PlantAction {
  void execute(Plant plant, Board board, int currentTick);

  /**
   * Ticks between one shot and the next, or 0 for a behaviour that does not work on a clock.
   *
   * <p>Read by the view so an attack animation can be played in the run-up to a shot rather than
   * after it: the plant winds up, and the projectile leaves on the frame the wind-up ends. Nothing
   * in the simulation reads it, and overriding it cannot change when a plant actually fires.
   */
  default int actionIntervalTicks() {
    return 0;
  }
}
