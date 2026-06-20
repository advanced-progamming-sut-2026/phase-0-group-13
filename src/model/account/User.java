package model.account;

import model.game.MatchResult;
import java.util.ArrayList;
import java.util.List;

public class User {
    // ---- اطلاعات حساب کاربری و احراز هویت ----
    private String username;
    private String passwordHash;
    private String email;
    private String nickname;
    private String gender;
    private String securityQuestionNumber;
    private String securityAnswer;

    // ---- اطلاعات و پیشرفت درون بازی ----
    private int coins;
    private int diamonds;
    private List<String> unlockedPlants;
    private List<String> unlockedZombies;
    private List<MatchResult> recentGames; // تاریخچه بازی‌ها

    // کانستراکتور دقیقاً منطبق با نیاز UserManager شما
    public User(String username, String passwordHash, String email, String nickname, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.nickname = nickname;
        this.gender = gender;

        // مقداردهی اولیه برای اطلاعات بازی
        this.coins = 0;
        this.diamonds = 0;
        this.unlockedPlants = new ArrayList<>();
        this.unlockedZombies = new ArrayList<>();
        this.recentGames = new ArrayList<>();

        // گیاهان پیش‌فرض که باز هستند
        this.unlockedPlants.add("peashooter");
        this.unlockedPlants.add("sunflower");
    }

    // متدهای مربوط به امنیت که UserManager شما صدا می‌زند
    public void setSecurityQuestion(String qNumber, String answer) {
        this.securityQuestionNumber = qNumber;
        this.securityAnswer = answer;
    }

    // متد اضافه کردن تاریخچه بازی با سقف ۴ بازی اخیر
    public void addMatchResult(MatchResult result) {
        if (result == null) return;
        recentGames.add(0, result);
    }

    // --- Getters & Setters ---
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
    public List<String> getUnlockedPlants() { return unlockedPlants; }
    public List<String> getUnlockedZombies() { return unlockedZombies; }
    public List<MatchResult> getRecentGames() { return recentGames; }
}