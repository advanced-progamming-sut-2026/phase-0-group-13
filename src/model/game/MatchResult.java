package model.game;

import java.util.ArrayList;
import java.util.List;
import model.game.reward.Reward;

public class MatchResult {
  private boolean won;
  private int score;
  private int rewardCoins;

  // transient: فقط وضعیت حین مچه؛ آخر مچ به کیف پول اعمال میشه. اگه توی Users.json ذخیره بشه
  // Gson موقع لود نمی‌تونه اینترفیس Reward رو بسازه و کل دیتابیس یوزرها می‌پره
  private transient List<Reward> earnedRewards = new ArrayList<>();

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
