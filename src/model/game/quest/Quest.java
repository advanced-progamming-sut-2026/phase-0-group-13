package model.game.quest;

import com.google.gson.annotations.SerializedName;
import model.account.User;

public class Quest {

  // فعلا ارتباط بین جیسون ها و دیتا سرور درست نشده
  // یعنی صرفا زده شده ساختار کلی جیسون

  // جیسون کوئست ها فارسیه
  // دیگه جای اینکه کل جیسون و عوض کنیم اسم هارو اینجا بهش میفهمونیم که چی چیه
  // اولش اگه فارسی نمیکردش ایعای از روی CSV نیازی به این مورد نبودش الان
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

  /** Clones a shared quest template into a fresh, per-user progress instance. */
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

  public String getTitle() {
    return title;
  }

  public String getCategory() {
    return category;
  }

  public String getCondition() {
    return condition;
  }

  public String getRewardType() {
    return rewardType;
  }

  public String getPriority() {
    return priority;
  }

  public String getVariable() {
    return variable;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public double getProgressOfQuest() {
    return progressOfQuest;
  }

  public boolean isRewardClaimed() {
    return rewardClaimed;
  }

  public void addProgress(double amount, double target) {
    if (isCompleted) return;

    progressOfQuest += amount;
    if (progressOfQuest >= target) {
      progressOfQuest = target;
      isCompleted = true;
      finish();
    }
  }

  public void start() {
    // مثلا وقتی این و بزنیم یه کوئست شروع میشه به بررسی شذن ( یعنی مثلا تو لیست کوست ها هستش و روند
    // شو چک میکنیم
    // که مثلا چند درصد پیشرفت هست یا هرچیزی
  }

  public void finish() {
    // وقتی کامل بشه اینجا یا خروجی ای میدیم یا هرچیزی
    this.isCompleted = true;
  }

  public void claimReward(User user) {
    if (!isCompleted || rewardClaimed) return;

    if (rewardType != null) {
      if (rewardType.contains("الماس") || rewardType.contains("Gem")) {
        int amount = extractNumber(rewardType);
        user.addDiamonds(amount);
      } else if (rewardType.contains("سکه") || rewardType.contains("Coin")) {
        int amount = extractNumber(rewardType);
        user.addCoins(amount);
      } else if (rewardType.contains("پک دانه")) {
        int amount = extractNumber(rewardType);
        user.getInventory().addItem("seed_packet", amount);
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
