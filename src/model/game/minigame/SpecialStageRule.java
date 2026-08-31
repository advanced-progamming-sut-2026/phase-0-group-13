package model.game.minigame;

import model.game.Board;
import model.game.GameState;

public interface SpecialStageRule {
  void apply(GameState gameState);

  default boolean isPlantAllowed(String plantName) {
    return true;
  }

  default boolean restrictsSelection() { return false; }

  // نوار نقالهٔ این مرحله، یا null. مرحلهٔ باس هم نوار دارد، پس صداکننده‌ها به‌جای instanceof
  default ConveyorRule belt() { return null; }
  default boolean checkLoseCondition(Board board) {
    return false;
  }

  default boolean checkWinCondition(Board board) {
    return false;
  }
}
