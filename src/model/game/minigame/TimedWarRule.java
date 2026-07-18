package model.game.minigame;

import model.game.Board;
import model.game.GameState;

public class TimedWarRule extends MiniGame implements SpecialStageRule {
  private final int timeLimitTicks;
  private int elapsedTicks;

  public TimedWarRule(int timeLimitTicks) {
    this.timeLimitTicks = timeLimitTicks;
  }

  @Override
  public void apply(GameState gameState) {
    elapsedTicks++;
  }

  // اگه زمان تموم بشه و هنوز زامبی زنده رو نقشه باشه، باخت
  @Override
  public boolean checkLoseCondition(Board board) {
    return elapsedTicks >= timeLimitTicks && !board.getZombies().isEmpty();
  }

  // اگه زمان تموم بشه و همه زامبی‌ها از بین رفته باشن، برد زودهنگام (نیازی به تموم شدن موج‌ها نیست)
  @Override
  public boolean checkWinCondition(Board board) {
    return elapsedTicks >= timeLimitTicks && board.getZombies().isEmpty();
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
