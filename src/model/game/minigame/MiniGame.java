package model.game.minigame;

import model.game.GameState;

public abstract class MiniGame {
  protected boolean started;
  protected int score; // معادل با MyoPoints در بازی‌های امتیازی

  public void start() {
    started = true;
  }

  public void end() {
    started = false;
  }

  public abstract boolean checkWinCondition();

  public boolean checkLoseCondition() {
    return false;
  }

  public void update(GameState gameState) {
  }

  public int getScore() {
    return score;
  }

  public void addScore(int points) {
    this.score += points;
  }
}