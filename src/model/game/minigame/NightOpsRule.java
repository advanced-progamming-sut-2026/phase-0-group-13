package model.game.minigame;

import model.game.GameState;

public class NightOpsRule extends MiniGame implements SpecialStageRule {
  @Override
  public void apply(GameState gameState) { gameState.setSkySunDisabled(true); }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
