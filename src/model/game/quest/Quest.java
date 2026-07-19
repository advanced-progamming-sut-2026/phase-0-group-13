package model.game.quest;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.account.User;
import model.enums.PlantTag;

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

  // ===========================================================================================
  // Contextual condition engine (the "14 quests" problem)
  // ---------------------------------------------------------------------------------------------
  // addProgress()/triggerQuestEvent() only ever answer "how many times did X happen?". A chunk
  // of quests instead ask "how was the match played?" (no sun spent at all, a symmetric garden,
  // a certain column left untouched, ...). Those can't be expressed as a running counter because
  // they depend on the state of the match as a whole, not on a stream of discrete events. This
  // engine reads that state from a MatchContext instead.
  //
  // Every entry below matches a distinctive substring of the Persian "شرط تکمیلی" (condition)
  // field from Quests.json against a predicate over MatchContext. This mirrors the keyword-match
  // style already used by User.QUEST_EVENT_KEYWORDS, just condition-based instead of event-based.
  // ===========================================================================================

  @FunctionalInterface
  private interface ContextCondition {
    boolean test(Quest quest, MatchContext context);
  }

  private static final Map<String, ContextCondition> CONTEXT_CONDITIONS = buildContextConditions();

  private static Map<String, ContextCondition> buildContextConditions() {
    Map<String, ContextCondition> map = new java.util.LinkedHashMap<>();
    map.put("صفر خورشید", (q, ctx) -> ctx.getSunSpent() == 0);

    map.put("۳۰ ثانیه", (q, ctx) -> ctx.getZombiesKilledInOpeningWindow() >= 10);

    map.put("گیاه انفجاری", (q, ctx) -> ctx.getExplosivePlantsPlaced() >= 3);

    map.put("به جز ردیف وسط", (q, ctx) -> ctx.isMatchWon() && !ctx.isGardenSymmetricExceptMiddleRow());

    map.put("متقارن باشد", (q, ctx) -> ctx.isGardenSymmetric());


    map.put("برای کشتن زامبی ها استفاده شود", (q, ctx) -> {
      PlantTag family = resolveFamilyTag(q.variable);
      return family != null && ctx.onlyPlacedFamily(family);
    });

    map.put("استفاده نشود", (q, ctx) -> {
      PlantTag family = resolveFamilyTag(q.variable);
      return ctx.isMatchWon() && family != null && ctx.neverPlacedFamily(family);
    });

    map.put("گیاهان شب", (q, ctx) ->
            ctx.neverPlacedFamily(PlantTag.DAY) && ctx.getPlantFamiliesPlaced().contains(PlantTag.SHROOM));

    map.put("پشت سر هم", (q, ctx) -> ctx.getWinStreakAtMaxDifficulty() >= 5);

    map.put("چمن‌زن ندارد", (q, ctx) -> ctx.getZombiesKilledAtColumnZeroWithNoMower() >= 10);

    map.put("گیاه تولیدکننده خورشید", (q, ctx) -> ctx.isMatchWon() && ctx.getSunProducerPlantsPlaced() == 3);

    map.put("ستون و ردیف", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isColumnEmpty(n - 1) && ctx.isRowEmpty(n - 1);
    });

    map.put("در ستون", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isColumnEmpty(n - 1);
    });

    map.put("سطر", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isRowEmpty(n - 1);
    });

    map.put("از دست دادن بیش از", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && ctx.getPlantsLost() <= Math.max(n, 0);
    });

    return map;
  }

  private static final Map<String, PlantTag> FAMILY_KEYWORDS = buildFamilyKeywords();

  private static Map<String, PlantTag> buildFamilyKeywords() {
    Map<String, PlantTag> map = new java.util.HashMap<>();

    return map;
  }

  private static PlantTag resolveFamilyTag(String variable) {
    if (variable == null) return null;
    String lower = variable.toLowerCase();
    for (PlantTag tag : PlantTag.values()) {
      if (lower.contains(tag.name().toLowerCase())) {
        return tag;
      }
    }
    return null;
  }

  private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");

  private static int extractFirstNumber(String text) {
    if (text == null) return -1;
    Matcher m = FIRST_NUMBER.matcher(text);
    return m.find() ? Integer.parseInt(m.group()) : -1;
  }


  public boolean checkCondition(MatchContext context) {
    if (isCompleted || context == null || condition == null) {
      return false;
    }
    for (Map.Entry<String, ContextCondition> entry : CONTEXT_CONDITIONS.entrySet()) {
      if (condition.contains(entry.getKey())) {
        if (entry.getValue().test(this, context)) {
          finish();
          return true;
        }
        return false;
      }
    }
    return false;
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