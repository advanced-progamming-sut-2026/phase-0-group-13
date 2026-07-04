package model.account;

import java.util.ArrayList;
import java.util.List;
import model.environment.greenhouse.GreenHouse;
import model.game.MatchResult;
import model.game.news.AllNews;
import model.game.quest.Quest;

public class User {
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

    this.unlockedPlants.add("peashooter");
    this.unlockedPlants.add("sunflower");
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

}