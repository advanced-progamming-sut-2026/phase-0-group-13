package model.game.quest;

import com.google.gson.annotations.SerializedName;
import model.account.User;

public class Quest {

  @SerializedName("نام کوئست ها")
  public String title;

  @SerializedName("دسته بندی")
  private String category;

  @SerializedName("شرط تکمیلی")
  private String condition;

  @SerializedName("نوع پاداش")
  private String rewardType;

  @SerializedName("اولویت")
  private String priority;

  @SerializedName("متغیر")
  private String variable;

  private double progressOfQuest;
  private boolean isCompleted;
  private boolean rewardClaimed;

  public Quest() {
    this.progressOfQuest = 0.0;
    this.isCompleted = false;
    this.rewardClaimed = false;
  }

  public Quest(Quest template) {
    this.title = template.title;
    this.category = template.category;
    this.condition = template.condition;
    this.rewardType = template.rewardType;
    this.priority = template.priority;
    this.variable = template.variable;
    this.progressOfQuest = 0.0;
    this.isCompleted = false;
    this.rewardClaimed = false;
  }

  public String getTitle() { return title; }
  public String getCategory() { return category; }
  public String getCondition() { return condition; }
  public String getRewardType() { return rewardType; }
  public String getPriority() { return priority; }
  public String getVariable() { return variable; }
  public boolean isCompleted() { return isCompleted; }
  public double getProgressOfQuest() { return progressOfQuest; }
  public boolean isRewardClaimed() { return rewardClaimed; }

  public void addProgress(double amount, double target) {
    if (isCompleted) return;

    progressOfQuest += amount;
    if (progressOfQuest >= target) {
      progressOfQuest = target;
      finish();
    }
  }

  public void start() {}

  public void finish() {
    this.isCompleted = true;
  }

  public void claimReward(User user) {
    if (!isCompleted || rewardClaimed) return;

    if (rewardType != null) {
      String rtLower = rewardType.toLowerCase();
      int amount = extractNumber(rewardType);

      if (rtLower.contains("الماس") || rtLower.contains("gem")) {
        user.addDiamonds(amount > 0 ? amount : 1);
      } else if (rtLower.contains("سکه") || rtLower.contains("coin")) {
        user.addCoins(amount > 0 ? amount : 100);
      }
      else if (rtLower.contains("پک دانه") || rtLower.contains("seed")) {
        String target = (variable != null && !variable.trim().isEmpty()) ? variable.trim() : "random";
        user.getInventory().addItem("seed_" + target, amount > 0 ? amount : 1);
      } else if (rtLower.contains("غذا") || rtLower.contains("food")) {
        user.getInventory().addItem("plant_food", amount > 0 ? amount : 1);
      }
      else if (rtLower.contains("آنلاک") || rtLower.contains("unlock") || rtLower.contains("باز کردن")) {
        if (variable != null && !variable.trim().isEmpty()) {
          user.unlockItem(variable.trim());
        }
      }
    }
    rewardClaimed = true;
  }

  private int extractNumber(String text) {
    try {
      String numberOnly = text.replaceAll("[^0-9]", "");
      return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
    } catch (Exception e) {
      return 0;
    }
  }
}