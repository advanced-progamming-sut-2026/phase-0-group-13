package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;


public class DummyPlantAction implements PlantAction {
  private final String reason;
  private boolean warned = false;

  public DummyPlantAction(String reason) {
    this.reason = reason;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (!warned) {
      System.out.printf(
          "[NOT IMPLEMENTED] %s has no real effect for: %s%n", plant.getName(), reason);
      warned = true;
    }
  }
}