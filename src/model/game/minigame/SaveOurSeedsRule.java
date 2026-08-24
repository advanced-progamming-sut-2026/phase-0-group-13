package model.game.minigame;

import model.game.Board;
import model.game.GameState;

public class SaveOurSeedsRule extends MiniGame implements SpecialStageRule {
  private java.util.List<model.game.plant.Plant> protectedPlants;

  @Override
  public void apply(GameState gameState) {}

  /** The plants the stage is guarding; empty until the first lose check has run. */
  public java.util.List<model.game.plant.Plant> getProtectedPlants() {
    return protectedPlants == null
            ? java.util.List.of() : java.util.Collections.unmodifiableList(protectedPlants);
  }

  // فقط گیاه‌های محافظت‌شده (همون‌هایی که موقع اولین چک رو زمین بودن) باخت رو رقم میزنن؛ گیاهی که
  // خود بازیکن بعدا کاشته و از بین رفته (مثل بمب گیلاس بعد از انفجار) شرط باخت نیست
  @Override
  public boolean checkLoseCondition(Board board) {
    if (protectedPlants == null) {
      protectedPlants = new java.util.ArrayList<>(board.getPlants());
      return false;
    }
    for (model.game.plant.Plant plant : protectedPlants) {
      if (plant.isDead()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
