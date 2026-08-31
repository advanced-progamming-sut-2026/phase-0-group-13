package model.account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Result;
import model.environment.greenhouse.GreenHouse;
import model.game.MatchResult;
import model.game.news.AllNews;
import model.game.quest.MatchContext;
import model.game.quest.Quest;

public class User {

  public static final int MIN_DECK_SLOTS = 6;
  public static final int MAX_DECK_SLOTS = 8;

  private String gender;

  private final List<String> unlockedPlants;
  private final List<String> unlockedZombies;
  private final List<MatchResult> recentGames;
  private final List<String> unlockedStages;
  private final Progress progress;
  private final Inventory inventory;
  private final GreenHouse greenHouse;
  private final AllNews newsBox;
  private final List<Quest> quests;

  private final Map<String, Integer> plantLevels;
  private final List<String> selectedDeck;
  private final List<String> boostedPlants;

  private String username;
  private String passwordHash;
  private String email;
  private String nickname;
  private String securityQuestionNumber;
  private String securityAnswer;
  private int coins;
  private int diamonds;
  private int difficultyLevel;
  private long lastShopRefreshTime;
  private long lastDailyDealPurchaseTime;
  private int meowPoints;
  private int winStreakAtMaxDifficulty;
  public static final int MAX_DIFFICULTY_LEVEL = 5;

  public User(String username, String passwordHash, String email, String nickname, String gender) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.nickname = nickname;
    this.gender = gender;

    this.coins = 0;
    this.diamonds = 0;
    this.difficultyLevel = 3;
    this.unlockedPlants = new ArrayList<>();
    this.unlockedZombies = new ArrayList<>();
    this.recentGames = new ArrayList<>();
    this.unlockedStages = new ArrayList<>();
    this.inventory = new Inventory();
    this.greenHouse = new GreenHouse();
    this.newsBox = new AllNews();
    this.quests = new ArrayList<>();
    this.lastShopRefreshTime = 0L;
    this.lastDailyDealPurchaseTime = 0L;
    this.meowPoints = 0;
    this.plantLevels = new HashMap<>();
    this.selectedDeck = new ArrayList<>();
    this.boostedPlants = new ArrayList<>();

    this.unlockedPlants.add("peashooter");
    this.unlockedPlants.add("sunflower");
    this.unlockedPlants.add("wall-nut");
    this.unlockedPlants.add("potato-mine");
    this.unlockedPlants.add("cabbage-pult");
    this.unlockedPlants.add("puff-shroom");

    for (String plant : this.unlockedPlants) {
      this.plantLevels.put(normalizePlantKey(plant), 1);
    }

    this.unlockedStages.add("stage_1");
    this.progress = new Progress();
  }

  public Result unlockAllChapters() {
    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      unlockItem("stage_" + stage);
    }
    return progress.unlockAllChapters();
  }

  public void unlockItem(String targetId) {
    if (targetId == null || targetId.isEmpty()) {
      return;
    }

    if (targetId.startsWith("plant_")) {
      String plantName = targetId.substring("plant_".length());
      unlockPlant(plantName);

    } else if (targetId.startsWith("zombie_")) {
      if (!unlockedZombies.contains(targetId)) {
        unlockedZombies.add(targetId);
        generateNews("zombie", targetId);
      }

    } else if (targetId.startsWith("stage_") || targetId.startsWith("minigame_")) {
      if (!unlockedStages.contains(targetId)) {
        unlockedStages.add(targetId);
        generateNews("stage", targetId);
      }
    }
  }

  public void setSecurityQuestion(String qNumber, String answer) {
    this.securityQuestionNumber = qNumber;
    this.securityAnswer = answer;
  }

  private void generateNews(String type, String targetId) {
    String message;
    switch (type) {
      case "plant":
        message = "New plant unlocked: " + readableName(targetId);
        break;
      case "zombie":
        message = "New zombie discovered: " + readableName(targetId);
        break;
      case "stage":
        message = "New stage available: " + readableName(targetId);
        break;
      case "quest":
        message = "Quest completed: " + targetId;
        break;
      default:
        message = "New item unlocked: " + targetId;
    }
    newsBox.addNews(new model.game.news.News(type, targetId, message));
  }

  private static String readableName(String targetId) {
    if (targetId == null || targetId.isBlank()) {
      return "";
    }
    String id = targetId.trim();
    if (id.startsWith("stage_")) {
      String tail = id.substring("stage_".length());
      try {
        String environment = AdventureMap.getEnvironmentForStage(Integer.parseInt(tail));
        if (environment != null) {
          return "Chapter " + tail + " - " + titleCase(environment.replace('_', ' '));
        }
      } catch (NumberFormatException ignored) {
      }
      return titleCase(tail.replace('_', ' '));
    }
    if (id.startsWith("minigame_")) {
      return titleCase(id.substring("minigame_".length()).replace('_', ' '));
    }
    if (id.startsWith("zombie_")) {
      return titleCase(splitAlias(id.substring("zombie_".length())));
    }
    if (id.startsWith("plant_")) {
      return titleCase(id.substring("plant_".length()).replace('_', ' '));
    }
    return titleCase(id.replace('_', ' '));
  }

  private static String splitAlias(String alias) {
    String cleaned = alias.toLowerCase();
    if (cleaned.startsWith("zombie")) {
      cleaned = cleaned.substring("zombie".length());
    }
    if (cleaned.endsWith("default")) {
      cleaned = cleaned.substring(0, cleaned.length() - "default".length());
    }
    if (cleaned.isBlank()) {
      cleaned = alias.toLowerCase();
    }
    for (String word : ALIAS_WORDS) {
      cleaned = cleaned.replace(word, " " + word + " ");
    }
    return cleaned.replaceAll("(\\d+)", " $1 ").replaceAll("\\s+", " ").trim();
  }

  private static final String[] ALIAS_WORDS = {
    "gargantuar", "zombotany", "zomboss", "peashooter", "prospector", "tombraiser", "troglobite",
    "fisherman", "snorkel", "octopus", "juggler", "jester", "wizard", "newspaper", "parasol",
    "turquoise", "explorer", "allstar", "arcade", "barrel", "roller", "mummy", "armor", "dodo",
    "hunter", "piano", "dragon", "skull", "crystal", "king", "imp", "mech", "dark", "egypt",
    "beach", "iceage", "modern", "lostcity", "eighties", "wallnut", "jalapeno", "squash", "pet",
    "tutorial"
  };

  private static String titleCase(String text) {
    String[] words = text.trim().split("\\s+");
    StringBuilder out = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      if (out.length() > 0) {
        out.append(' ');
      }
      out.append(Character.toUpperCase(word.charAt(0)))
          .append(word.substring(1).toLowerCase());
    }
    return out.toString();
  }

  public void addMatchResult(MatchResult result) {
    if (result == null) return;
    recentGames.add(0, result);
  }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getSecurityQuestionNumber() { return securityQuestionNumber; }
  public String getSecurityAnswer() { return securityAnswer; }
  public int getCoins() { return coins; }

  public void addCoins(int amount) {
    this.coins = Math.max(0, this.coins + amount);
  }

  public void addDiamonds(int amount) {
    this.diamonds = Math.max(0, this.diamonds + amount);
  }

  public int getDiamonds() { return diamonds; }
  public int getDifficultyLevel() { return difficultyLevel; }
  public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }
  public List<String> getUnlockedPlants() { return unlockedPlants; }
  public List<String> getUnlockedZombies() { return unlockedZombies; }
  public List<MatchResult> getRecentGames() { return recentGames; }
  public Progress getProgress() { return progress; }
  public List<String> getUnlockedStages() { return unlockedStages; }
  public Inventory getInventory() { return inventory; }
  public GreenHouse getGreenHouse() { return greenHouse; }
  public AllNews getNewsBox() { return newsBox; }
  public List<Quest> getQuests() { return quests; }
  public long getLastShopRefreshTime() { return lastShopRefreshTime; }
  public void setLastShopRefreshTime(long lastShopRefreshTime) { this.lastShopRefreshTime = lastShopRefreshTime; }
  public long getLastDailyDealPurchaseTime() { return lastDailyDealPurchaseTime; }
  public void setLastDailyDealPurchaseTime(long lastDailyDealPurchaseTime) {
    this.lastDailyDealPurchaseTime = lastDailyDealPurchaseTime; }
  public int getMeowPoints() { return meowPoints; }

  public void addMeowPoints(int amount) {
    this.meowPoints += amount;
  }

  public boolean hasUnlockedPlant(String plantName) {
    if (plantName == null) {
      return false;
    }
    String key = normalizePlantKey(plantName);
    for (String owned : unlockedPlants) {
      if (normalizePlantKey(owned).equals(key)) {
        return true;
      }
    }
    return false;
  }

  public static String normalizePlantKey(String plantName) {
    return plantName.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  public Result unlockPlant(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (hasUnlockedPlant(key)) {
      return new Result(false, "You already own " + plantName + "!", null);
    }

    unlockedPlants.add(key);
    plantLevels.put(normalizePlantKey(key), 1);
    generateNews("plant", key);
    triggerQuestEvent("PLANT_UNLOCKED", 1);
    return new Result(true, plantName + " unlocked and added to your collection!", key);
  }

  public int getPlantLevel(String plantName) {
    if (plantName == null || !hasUnlockedPlant(plantName)) {
      return 0;
    }
    return plantLevels.getOrDefault(normalizePlantKey(plantName), 1);
  }

  public void setPlantLevel(String plantName, int level) {
    if (plantName == null) {
      return;
    }
    plantLevels.put(normalizePlantKey(plantName), level);
  }

  public Result addToDeck(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (!hasUnlockedPlant(key)) {
      return new Result(false, "error: you haven't unlocked " + plantName + " yet", null);
    }
    if (selectedDeck.contains(key)) {
      return new Result(false, plantName + " is already in your seed bank.", null);
    }
    if (selectedDeck.size() >= MAX_DECK_SLOTS) {
      return new Result(
              false, "error: seed bank is full (maximum " + MAX_DECK_SLOTS + " plants)", null);
    }

    selectedDeck.add(key);
    return new Result(
            true,
            plantName + " added to the seed bank (" + selectedDeck.size() + "/" + MAX_DECK_SLOTS + ").",
            selectedDeck.size());
  }

  public Result removeFromDeck(String plantName) {
    if (plantName == null) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (!selectedDeck.remove(key)) {
      return new Result(false, plantName + " is not in your seed bank.", null);
    }
    boostedPlants.remove(key);
    return new Result(true, plantName + " removed from the seed bank.", selectedDeck.size());
  }

  public void clearDeck() {
    selectedDeck.clear();
    boostedPlants.clear();
  }

  public List<String> getSelectedDeck() {
    return selectedDeck;
  }

  public Result boostPlant(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (!hasUnlockedPlant(key)) {
      return new Result(false, "error: you haven't unlocked " + plantName + " yet", null);
    }
    if (boostedPlants.contains(key)) {
      return new Result(false, plantName + " is already boosted.", null);
    }
    if (diamonds < 2) {
      return new Result(false, "error: not enough diamonds (2 required)", null);
    }

    diamonds -= 2;
    boostedPlants.add(key);
    return new Result(
            true,
            plantName + " boosted! Its Plant Food effect will trigger instantly once planted.",
            key);
  }

  public static final int UPGRADE_COIN_COST = 500;
  public static final int UPGRADE_SEED_COST = 10;
  public static final int MAX_PLANT_LEVEL = 4;

  public int upgradeSeedCost(String plantName) {
    return UPGRADE_SEED_COST * getPlantLevel(plantName);
  }

  public int upgradeCoinCost(String plantName) {
    return UPGRADE_COIN_COST * getPlantLevel(plantName);
  }

  public Result upgradePlant(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (!hasUnlockedPlant(key)) {
      return new Result(false, "error: you haven't unlocked " + plantName + " yet", null);
    }

    int currentLevel = getPlantLevel(key);
    if (currentLevel >= MAX_PLANT_LEVEL) {
      return new Result(false, "error: " + plantName + " is already at the maximum level", null);
    }

    String seedKey = "seed_" + key;
    int seedCost = UPGRADE_SEED_COST * currentLevel;
    int coinCost = UPGRADE_COIN_COST * currentLevel;

    if (getInventory().getItemCount(seedKey) < seedCost) {
      return new Result(false,
              "error: not enough seed packets for " + plantName + " (need " + seedCost + ")", null);
    }
    if (getCoins() < coinCost) {
      return new Result(false, "error: not enough coins (need " + coinCost + ")", null);
    }

    getInventory().consumeItem(seedKey, seedCost);
    addCoins(-coinCost);
    int newLevel = currentLevel + 1;
    setPlantLevel(key, newLevel);
    return new Result(true, plantName + " upgraded to level " + newLevel + "!", key);
  }

  public boolean isPlantBoosted(String plantName) {
    return plantName != null && boostedPlants.contains(plantName.toLowerCase().trim());
  }

  public List<String> getBoostedPlants() {
    return boostedPlants;
  }

  public Result addFreeBoost(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }
    String key = plantName.toLowerCase().trim();
    if (!hasUnlockedPlant(key)) {
      return new Result(false, "error: you haven't unlocked " + plantName + " yet", null);
    }
    if (boostedPlants.contains(key)) {
      return new Result(false, plantName + " is already boosted.", null);
    }

    boostedPlants.add(key);
    return new Result(true, plantName + " free boost stored!", key);
  }

  private static final String[] QUEST_EVENT_PRIORITY = {
    "MINIGAME_CLEAR", "PLANT_UNLOCKED", "PLANT_PURCHASED", "COLLECT_SUN", "STAGE_CLEAR", "KILL_ZOMBIE"
  };

  private static final Map<String, String[]> QUEST_EVENT_KEYWORDS = buildQuestEventKeywords();
  private static final Map<String, Integer> DEFAULT_QUEST_TARGETS = buildDefaultQuestTargets();

  private static Map<String, String[]> buildQuestEventKeywords() {
    Map<String, String[]> map = new HashMap<>();
    map.put("KILL_ZOMBIE", new String[] {"defeat", "zombie"});
    map.put("COLLECT_SUN", new String[] {"sun_amount", "collect"});
    map.put("STAGE_CLEAR", new String[] {"clear a chapter", "clear a stage", "beat a level"});
    map.put("MINIGAME_CLEAR", new String[] {"mini-game", "mini game", "minigame"});
    map.put("PLANT_UNLOCKED", new String[] {"unlock a new plant", "unlock plant"});
    map.put("PLANT_PURCHASED", new String[] {"purchase", "buy a plant"});
    return map;
  }

  private static Map<String, Integer> buildDefaultQuestTargets() {
    Map<String, Integer> map = new HashMap<>();
    map.put("KILL_ZOMBIE", 50);
    map.put("COLLECT_SUN", 3000);
    map.put("STAGE_CLEAR", 1);
    map.put("MINIGAME_CLEAR", 1);
    map.put("PLANT_UNLOCKED", 1);
    map.put("PLANT_PURCHASED", 1);
    return map;
  }

  private static String resolveEventType(Quest quest) {
    if (quest.getCondition() == null || quest.isContextual()) {
      return null;
    }
    String haystack =
            (quest.getCondition() + " " + (quest.getCategory() != null ? quest.getCategory() : ""))
                    .toLowerCase();

    for (String eventType : QUEST_EVENT_PRIORITY) {
      for (String keyword : QUEST_EVENT_KEYWORDS.get(eventType)) {
        if (haystack.contains(keyword.toLowerCase())) {
          return eventType;
        }
      }
    }
    return null;
  }



  public void seedQuestsIfNeeded(List<Quest> templates) {
    if (templates == null || templates.isEmpty()) {
      return;
    }
    boolean broken = false;
    for (Quest quest : quests) {
      if (quest == null || quest.getTitle() == null || quest.getCondition() == null) {
        broken = true;
        break;
      }
    }
    if (!quests.isEmpty() && !broken) {
      // Quests.json since this account was seeded are appended. Without this an existing save
      // never sees a newly added category at all -- its travel-log page would be empty for
      addMissingQuests(templates);
      return;
    }

    quests.clear();
    for (Quest template : templates) {
      quests.add(new Quest(template));
    }
  }

  private void addMissingQuests(List<Quest> templates) {
    Set<String> owned = new HashSet<>();
    for (Quest quest : quests) {
      owned.add(quest.getTitle());
    }
    for (Quest template : templates) {
      if (template != null && template.getTitle() != null && owned.add(template.getTitle())) {
        quests.add(new Quest(template));
      }
    }
  }

  public void evaluateContextualQuests(MatchContext context) {
    if (context == null) {
      return;
    }
    context.setWinStreakAtMaxDifficulty(winStreakAtMaxDifficulty);
    for (Quest quest : quests) {
      if (quest.isCompleted()) {
        continue;
      }
      if (quest.checkCondition(context)) {
        generateNews("quest", quest.getTitle());
      }
    }
  }

  public void updateDifficultyWinStreak(boolean won) {
    if (won && difficultyLevel >= MAX_DIFFICULTY_LEVEL) {
      winStreakAtMaxDifficulty++;
    } else if (!won) {
      winStreakAtMaxDifficulty = 0;
    }
  }

  public int getWinStreakAtMaxDifficulty() {
    return winStreakAtMaxDifficulty;
  }

  public void triggerQuestEvent(String eventType, int amount) {
    if (eventType == null || amount <= 0) {
      return;
    }
    String normalizedEvent = eventType.toUpperCase().trim();
    if (!QUEST_EVENT_KEYWORDS.containsKey(normalizedEvent)) {
      return;
    }

    for (Quest quest : quests) {
      if (quest.isCompleted() || !normalizedEvent.equals(resolveEventType(quest))) {
        continue;
      }

      double target = resolveQuestTarget(quest, normalizedEvent);
      quest.addProgress(amount, target);
      if (quest.isCompleted()) {
        generateNews("quest", quest.getTitle());
      }
    }
  }

  private double resolveQuestTarget(Quest quest, String eventType) {
    if (quest.getVariable() != null) {
      Matcher numberMatcher = Pattern.compile("\\d+").matcher(quest.getVariable());
      if (numberMatcher.find()) {
        try {
          return Double.parseDouble(numberMatcher.group());
        } catch (NumberFormatException ignored) {
        }
      }
    }
    Integer fallback = DEFAULT_QUEST_TARGETS.get(eventType.toUpperCase().trim());
    return fallback != null ? fallback : 1;
  }
}
