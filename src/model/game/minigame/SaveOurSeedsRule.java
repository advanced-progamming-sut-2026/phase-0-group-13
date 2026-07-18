package model.game.minigame;

import model.game.Board;
import model.game.GameState;

public class SaveOurSeedsRule extends MiniGame implements SpecialStageRule {
  private int lastKnownPlantCount = -1;

  @Override
  public void apply(GameState gameState) {}

  // اگه هر گیاهی از دست بره (خورده بشه یا حتی pluck بشه)، باخت فوریه. لحظه اولی که این چک صدا زده
  // میشه فقط شمارش اولیه رو ثبت میکنه، هنوز باخت حساب نمیشه
  @Override
  public boolean checkLoseCondition(Board board) {
    int currentCount = board.getPlants().size();
    if (lastKnownPlantCount == -1) {
      lastKnownPlantCount = currentCount;
      return false;
    }
    boolean lost = currentCount < lastKnownPlantCount;
    lastKnownPlantCount = currentCount;
    return lost;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
