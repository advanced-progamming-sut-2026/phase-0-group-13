package model.game.minigame;

import model.game.Board;
import model.game.GameState;
import model.game.zombie.Zombie;

public class DeadLineRule extends MiniGame implements SpecialStageRule {
  private final int deadlineColumn;

  public DeadLineRule(int deadlineColumn) {
    this.deadlineColumn = deadlineColumn;
  }

  public int getDeadlineColumn() { return deadlineColumn; }
  @Override
  public void apply(GameState gameState) {}

  @Override
  public boolean checkLoseCondition(Board board) {
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombie.getX() <= deadlineColumn) {
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
