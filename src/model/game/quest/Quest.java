package model.game.quest;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.account.User;
import model.enums.PlantTag;

public class Quest {

  @SerializedName("Quest Name")
  public String title;

  @SerializedName("Category")
  private String category;

  @SerializedName("Completion Condition")
  private String condition;

  @SerializedName("Reward")
  private String rewardType;

  @SerializedName("Priority")
  private String priority;

  @SerializedName("Variable")
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

  private static final String[] PLACEHOLDERS = {"sun_amount", "family_type", "n"};

  /**
   * متن شرط برای نمایش، با جای‌گذاری Variable روی جاهای خالیِ Quests.json. getCondition() دست
   * نمی‌خورد چون موتور تطبیق کوئست‌ها روی همان رشته‌ی خام کلید می‌خورد.
   */
  public String getDescription() {
    if (condition == null) {
      return null;
    }
    if (variable == null || variable.isBlank()) {
      return condition;
    }
    // "from the chapter chapter": اولی جای‌خالی است، دومی خودِ کلمه
    String text =
        condition.replaceAll("\\bchapter chapter\\b", Matcher.quoteReplacement(variable) + " chapter");
    for (String token : PLACEHOLDERS) {
      text = text.replaceAll("\\b" + token + "\\b", Matcher.quoteReplacement(variable));
    }
    return text;
  }

  private double questTarget;

  public double getQuestTarget() { return questTarget; }

  public void addProgress(double amount, double target) {
    if (isCompleted) return;

    this.questTarget = target;
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
  // Every entry below matches a distinctive substring of the "Completion Condition" field from
  // Quests.json against a predicate over MatchContext. This mirrors the keyword-match style
  // already used by User.QUEST_EVENT_KEYWORDS, just condition-based instead of event-based.
  // ===========================================================================================

  @FunctionalInterface
  private interface ContextCondition {
    boolean test(Quest quest, MatchContext context);
  }

  private static final int KILL_GOAL = 10;
  private static final int EXPLOSIVE_GOAL = 3;
  private static final int SUN_PRODUCER_GOAL = 3;
  private static final int WIN_STREAK_GOAL = 5;

  private static final Map<String, ContextCondition> CONTEXT_CONDITIONS = buildContextConditions();

  private static Map<String, ContextCondition> buildContextConditions() {
    Map<String, ContextCondition> map = new java.util.LinkedHashMap<>();
    putCombatConditions(map);
    putGardenConditions(map);
    putProgressConditions(map);
    return map;
  }

  private static void putCombatConditions(Map<String, ContextCondition> map) {
    map.put("using only the cactus", (q, ctx) ->
            ctx.getTotalZombiesKilled() >= KILL_GOAL && ctx.onlyPlacedPlant("cactus"));

    map.put("using only plant", (q, ctx) ->
            ctx.getTotalZombiesKilled() >= KILL_GOAL && ctx.placedExactlyOnePlantType());

    map.put("only the lawn mower", (q, ctx) -> ctx.onlyMowerKills());

    map.put("zero sun spent", (q, ctx) -> ctx.isMatchWon() && ctx.getSunSpent() == 0);

    map.put("within 30 seconds", (q, ctx) -> ctx.getZombiesKilledInOpeningWindow() >= KILL_GOAL);

    map.put("explosive plants", (q, ctx) -> ctx.getExplosivePlantsPlaced() >= EXPLOSIVE_GOAL);
  }

  private static void putGardenConditions(Map<String, ContextCondition> map) {
    map.put("no symmetry", (q, ctx) ->
            ctx.isMatchWon()
                    && ctx.getPlantsPlacedCount() > 0
                    && !ctx.isGardenSymmetricExceptMiddleRow());

    // "The final garden layout must be symmetric": باید در پایان مسابقه (نه هر لحظه از وسط بازی) و
    // روی چیدمانی که واقعا روی زمین ایستاده سنجیده شود، وگرنه یک گیاه در ردیف وسط یا زمینِ خالی
    // (بعد از خورده‌شدن همه‌ی گیاه‌ها) هم کوئست را کامل می‌کرد
    map.put("must be symmetric", (q, ctx) ->
            ctx.isMatchWon() && ctx.getPlantsOnBoard() > 0 && ctx.isGardenSymmetric());

    map.put("without using any plant of the", (q, ctx) -> {
      PlantTag family = resolveFamilyTag(q.variable);
      return ctx.isMatchWon() && family != null && ctx.neverPlacedFamily(family);
    });

    map.put("Use only plants of the", (q, ctx) -> {
      PlantTag family = resolveFamilyTag(q.variable);
      return family != null
              && ctx.getTotalZombiesKilled() > 0
              && ctx.onlyPlacedFamily(family);
    });

    map.put("night plants", (q, ctx) ->
            ctx.isMatchWon()
                    && ctx.neverPlacedFamily(PlantTag.DAY)
                    && ctx.getPlantFamiliesPlaced().contains(PlantTag.SHROOM));
  }

  private static void putProgressConditions(Map<String, ContextCondition> map) {
    map.put("levels in a row", (q, ctx) -> ctx.getWinStreakAtMaxDifficulty() >= WIN_STREAK_GOAL);

    map.put("with no lawn mower", (q, ctx) ->
            ctx.getZombiesKilledAtColumnZeroWithNoMower() >= KILL_GOAL);

    map.put("sun-producing plants", (q, ctx) ->
            ctx.isMatchWon() && ctx.getSunProducerPlantsPlaced() == SUN_PRODUCER_GOAL);

    map.put("both column n and row n", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isColumnEmpty(n - 1) && ctx.isRowEmpty(n - 1);
    });

    map.put("in column n", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isColumnEmpty(n - 1);
    });

    map.put("in row n", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && n > 0 && ctx.isRowEmpty(n - 1);
    });

    map.put("without losing more than", (q, ctx) -> {
      int n = extractFirstNumber(q.variable);
      return ctx.isMatchWon() && ctx.getPlantsLost() <= Math.max(n, 0);
    });
  }

  private static final Map<String, PlantTag> FAMILY_KEYWORDS = buildFamilyKeywords();

  private static Map<String, PlantTag> buildFamilyKeywords() {
    Map<String, PlantTag> map = new java.util.LinkedHashMap<>();

    map.put("pea", PlantTag.PEA);
    map.put("mushroom", PlantTag.SHROOM);
    map.put("shroom", PlantTag.SHROOM);
    map.put("ice", PlantTag.ICE);
    map.put("frost", PlantTag.ICE);
    map.put("fire", PlantTag.FIRE);
    map.put("poison", PlantTag.POISON);
    map.put("water", PlantTag.WATER);
    map.put("aquatic", PlantTag.WATER);
    map.put("sun-producing", PlantTag.SUN);
    map.put("sun", PlantTag.SUN);
    map.put("explosive", PlantTag.EXPLOSIVE);
    map.put("night", PlantTag.NIGHT);
    map.put("day", PlantTag.DAY);
    map.put("area", PlantTag.AOE);
    map.put("aoe", PlantTag.AOE);
    map.put("trap", PlantTag.TRAP);
    map.put("magic", PlantTag.MAGIC);
    map.put("stack", PlantTag.STACK);
    map.put("charge", PlantTag.CHARGE);
    map.put("ramp", PlantTag.RAMP_UP);
    map.put("move", PlantTag.MOVE_ZOMBIES);

    return map;
  }

  private static PlantTag resolveFamilyTag(String variable) {
    if (variable == null) return null;
    String lower = variable.trim().toLowerCase();

    for (Map.Entry<String, PlantTag> entry : FAMILY_KEYWORDS.entrySet()) {
      if (lower.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    for (PlantTag tag : PlantTag.values()) {
      if (lower.contains(tag.name().toLowerCase())
              || lower.contains(tag.name().toLowerCase().replace('_', ' '))) {
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

  public boolean isContextual() {
    if (condition == null) return false;
    for (String key : CONTEXT_CONDITIONS.keySet()) {
      if (condition.contains(key)) return true;
    }
    return false;
  }

  private static final Random REWARD_RANDOM = new Random();

  public void claimReward(User user) {
    if (!isCompleted || rewardClaimed) return;

    if (rewardType != null) {
      String rtLower = rewardType.toLowerCase();
      int amount = resolveRewardAmount();

      if (rtLower.contains("gem") || rtLower.contains("diamond")) {
        user.addDiamonds(amount > 0 ? amount : 1);
      } else if (rtLower.contains("coin")) {
        user.addCoins(amount > 0 ? amount : 100);
      } else if (rtLower.contains("seed")) {
        user.getInventory().addItem("seed_" + resolveSeedTarget(user), amount > 0 ? amount : 1);
      } else if (rtLower.contains("plant food")) {
        user.getInventory().addItem("plant_food", amount > 0 ? amount : 1);
      } else if (rtLower.contains("new plant")) {
        unlockRandomPlant(user);
      } else if (rtLower.contains("unlock")) {
        if (variable != null && !variable.trim().isEmpty()) {
          user.unlockItem(variable.trim());
        }
      }
    }
    rewardClaimed = true;
  }

  private int resolveRewardAmount() {
    if (rewardType != null && rewardType.toLowerCase().contains("sun_amount")) {
      int sunTarget = extractFirstNumber(variable);
      int divisor = extractFirstNumber(rewardType);
      if (sunTarget > 0 && divisor > 0) {
        return Math.max(1, sunTarget / divisor);
      }
    }
    return extractNumber(rewardType);
  }

  private String resolveSeedTarget(User user) {
    if (variable != null && user.hasUnlockedPlant(variable.trim())) {
      return variable.trim().toLowerCase();
    }
    List<String> owned = user.getUnlockedPlants();
    if (owned.isEmpty()) {
      return "random";
    }
    return owned.get(REWARD_RANDOM.nextInt(owned.size()));
  }

  private void unlockRandomPlant(User user) {
    if (data.GameDataManager.plantRepository == null) {
      return;
    }
    List<model.game.plant.PlantParts.PlantTemplate> locked =
            data.GameDataManager.plantRepository.getAll().stream()
                    .filter(t -> t.name != null && !user.hasUnlockedPlant(t.name))
                    .toList();
    if (locked.isEmpty()) {
      return;
    }
    String plantName = locked.get(REWARD_RANDOM.nextInt(locked.size())).name;
    System.out.println(user.unlockPlant(plantName).message());
  }

  private int extractNumber(String text) {
    if (text == null) return 0;
    try {
      String numberOnly = text.replaceAll("[^0-9]", "");
      return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
    } catch (Exception e) {
      return 0;
    }
  }
}
