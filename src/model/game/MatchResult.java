package model.game;

import java.util.ArrayList;
import java.util.List;
import model.game.reward.Reward;

public class MatchResult {
  private boolean won;
  private int score;
  private int rewardCoins;
  private final List<Reward> earnedRewards = new ArrayList<>();

  public void markWin() {
    won = true;
  }

  public void markLose() {
    won = false;
  }

  public void calculateRewards() {}

  // زامبی‌هایی که وسط بازی از خودشون دراپ انداختن (Board.drainPendingRewards) اینجا انباشته میشن؛
  // اعمال واقعی این جایزه‌ها رو یوزر (calculateRewards/applyScoresToUser) هنوز کار GamePlayController
  // موقع پایان مچه
  public void addEarnedReward(Reward reward) {
    if (reward != null) {
      earnedRewards.add(reward);
    }
  }

  public List<Reward> getEarnedRewards() {
    return earnedRewards;
  }

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
