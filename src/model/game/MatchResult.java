package model.game;

import java.util.ArrayList;
import java.util.List;
import model.game.reward.Reward;

public class MatchResult {
  private boolean won;
  private int score;
  private int rewardCoins;

  private transient List<Reward> earnedRewards = new ArrayList<>();

  public void markWin() {
    won = true;
  }

  public void markLose() {
    won = false;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public static final int COINS_PER_WAVE_CLEARED = 50;

  public void calculateRewards(int wavesCleared) {
    if (!won) {
      rewardCoins = 0;
      return;
    }
    rewardCoins = Math.max(1, wavesCleared) * COINS_PER_WAVE_CLEARED;
    addEarnedReward(new model.game.reward.Currency("COIN", rewardCoins));
  }

  public void addEarnedReward(Reward reward) {
    if (reward == null) {
      return;
    }
    if (earnedRewards == null) {
      earnedRewards = new ArrayList<>();
    }
    earnedRewards.add(reward);
  }

  public List<Reward> getEarnedRewards() {
    return earnedRewards != null ? earnedRewards : new ArrayList<>();
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
