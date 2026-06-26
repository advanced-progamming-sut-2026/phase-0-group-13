package model.game;

public class MatchResult {
  private boolean won;
  private int score;
  private int rewardCoins;

  public void markWin() {
    won = true;
  }

  public void markLose() {
    won = false;
  }

  public void calculateRewards() {}

  public boolean isWon() {
    return won;
  }

  public int getScore() {
    return score;
  }

  public int getRewardCoins() {
    return rewardCoins;
  }
}
