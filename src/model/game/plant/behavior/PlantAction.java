package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

public interface PlantAction {
  void execute(Plant plant, Board board, int currentTick);
}
