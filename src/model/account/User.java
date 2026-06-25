package model.account;

import model.game.MatchResult;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String passwordHash;
    private String email;
    private String nickname;
    private String gender;
    private String securityQuestionNumber;
    private String securityAnswer;

    private int coins;
    private int diamonds;
    private int difficultyLevel;
    private List<String> unlockedPlants;
    private List<String> unlockedZombies;
    private List<MatchResult> recentGames;
    private Progress progress;

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

        this.unlockedPlants.add("peashooter");
        this.unlockedPlants.add("sunflower");
        this.progress = new Progress();
    }

    public void setSecurityQuestion(String qNumber, String answer) {
        this.securityQuestionNumber = qNumber;
        this.securityAnswer = answer;
    }

    public void addMatchResult(MatchResult result) {
        if (result == null) return;
        recentGames.add(0, result);
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getGender() { return gender; }
    public String getSecurityQuestionNumber() { return securityQuestionNumber; }
    public String getSecurityAnswer() { return securityAnswer; }

    public int getCoins() { return coins; }
    public void addCoins(int amount) { this.coins += amount; }
    public int getDiamonds() { return diamonds; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public List<String> getUnlockedPlants() { return unlockedPlants; }
    public List<String> getUnlockedZombies() { return unlockedZombies; }
    public List<MatchResult> getRecentGames() { return recentGames; }
    public Progress getProgress() { return progress;}
}