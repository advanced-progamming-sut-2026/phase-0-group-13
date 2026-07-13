package model.account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Result;
import model.environment.greenhouse.GreenHouse;
import model.game.MatchResult;
import model.game.news.AllNews;
import model.game.quest.Quest;

public class User {

  public static final int MIN_DECK_SLOTS = 6;
  public static final int MAX_DECK_SLOTS = 7;

  private final String gender;

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
  private int meowPoints;

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
    this.meowPoints = 0;
    this.plantLevels = new HashMap<>();
    this.selectedDeck = new ArrayList<>();
    this.boostedPlants = new ArrayList<>();

    this.unlockedPlants.add("peashooter");
    this.unlockedPlants.add("sunflower");
    this.unlockedStages.add("stage_1");
    this.progress = new Progress();
  }

  public void unlockItem(String targetId) {
    if (targetId == null || targetId.isEmpty()) {
      return;
    }

    if (hasAlreadyUnlocked(targetId)) {
      return;
    }

    if (targetId.startsWith("plant_")) {
      unlockedPlants.add(targetId);
      generateNews("plant", targetId);

    } else if (targetId.startsWith("zombie_")) {
      unlockedZombies.add(targetId);
      generateNews("zombie", targetId);

    } else if (targetId.startsWith("stage_") || targetId.startsWith("minigame_")) {
      unlockedStages.add(targetId);
      generateNews("stage", targetId);
    }
  }

  private boolean hasAlreadyUnlocked(String targetId) {
    return unlockedPlants.contains(targetId)
        || unlockedZombies.contains(targetId)
        || unlockedStages.contains(targetId);
  }

  public void setSecurityQuestion(String qNumber, String answer) {
    this.securityQuestionNumber = qNumber;
    this.securityAnswer = answer;
  }

  private void generateNews(String type, String targetId) {
    String message;
    switch (type) {
      case "plant":
        message = "New plant unlocked: " + targetId;
        break;
      case "zombie":
        message = "New zombie discovered: " + targetId;
        break;
      case "stage":
        message = "New stage available: " + targetId;
        break;
      default:
        message = "New item unlocked: " + targetId;
    }
    newsBox.addNews(new model.game.news.News(type, targetId, message));
  }

  public void addMatchResult(MatchResult result) {
    if (result == null) return;
    recentGames.add(0, result);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getGender() {
    return gender;
  }

  public String getSecurityQuestionNumber() {
    return securityQuestionNumber;
  }

  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public int getCoins() {
    return coins;
  }

  public void addCoins(int amount) {
    this.coins += amount;
  }

  public void addDiamonds(int amount) {
    this.diamonds += amount;
  }

  public int getDiamonds() {
    return diamonds;
  }

  public int getDifficultyLevel() {
    return difficultyLevel;
  }

  public void setDifficultyLevel(int difficultyLevel) {
    this.difficultyLevel = difficultyLevel;
  }

  public List<String> getUnlockedPlants() {
    return unlockedPlants;
  }

  public List<String> getUnlockedZombies() {
    return unlockedZombies;
  }

  public List<MatchResult> getRecentGames() {
    return recentGames;
  }

  public Progress getProgress() {
    return progress;
  }

  public List<String> getUnlockedStages() {
    return unlockedStages;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public GreenHouse getGreenHouse() {
    return greenHouse;
  }

  public AllNews getNewsBox() {
    return newsBox;
  }

  public List<Quest> getQuests() {
    return quests;
  }

  public long getLastShopRefreshTime() {
    return lastShopRefreshTime;
  }

  public void setLastShopRefreshTime(long lastShopRefreshTime) {
    this.lastShopRefreshTime = lastShopRefreshTime;
  }

  public int getMeowPoints() {
    return meowPoints;
  }

  public void addMeowPoints(int amount) {
    this.meowPoints += amount;
  }

  public boolean hasUnlockedPlant(String plantName) {
    return plantName != null && unlockedPlants.contains(plantName.toLowerCase().trim());
  }

  public Result unlockPlant(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "error: invalid plant name", null);
    }

    String key = plantName.toLowerCase().trim();
    if (unlockedPlants.contains(key)) {
      return new Result(false, "You already own " + plantName + "!", null);
    }

    unlockedPlants.add(key);
    plantLevels.put(key, 1);
    generateNews("plant", key);
    return new Result(true, plantName + " unlocked and added to your collection!", key);
  }

  public int getPlantLevel(String plantName) {
    if (plantName == null || !hasUnlockedPlant(plantName)) {
      return 0;
    }
    return plantLevels.getOrDefault(plantName.toLowerCase().trim(), 1);
  }

  public void setPlantLevel(String plantName, int level) {
    if (plantName == null) {
      return;
    }
    plantLevels.put(plantName.toLowerCase().trim(), level);
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
}
