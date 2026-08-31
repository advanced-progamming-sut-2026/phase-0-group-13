package network.server;

import com.google.gson.JsonElement;

public class ServerAccount {

  private String username;
  private String passwordHash;
  private String nickname;
  private String email;
  private String gender;
  private String securityQuestionNumber;
  private String securityAnswer;
  private String sessionToken;
  private int coins;
  private int diamonds;
  private Integer bestScore;
  private JsonElement gameData;

  public ServerAccount() {}

  public ServerAccount(
      String username, String passwordHash, String nickname, String email, String gender) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.nickname = nickname;
    this.email = email;
    this.gender = gender;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getSecurityQuestionNumber() {
    return securityQuestionNumber;
  }

  public void setSecurityQuestionNumber(String securityQuestionNumber) {
    this.securityQuestionNumber = securityQuestionNumber;
  }

  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  public String getSessionToken() {
    return sessionToken;
  }

  public void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getGender() {
    return gender;
  }

  public int getCoins() {
    return coins;
  }

  public void setCoins(int coins) {
    this.coins = coins;
  }

  public int getDiamonds() {
    return diamonds;
  }

  public void setDiamonds(int diamonds) {
    this.diamonds = diamonds;
  }

  public Integer getBestScore() {
    return bestScore;
  }

  public void setBestScore(Integer bestScore) {
    this.bestScore = bestScore;
  }

  public JsonElement getGameData() {
    return gameData;
  }

  public void setGameData(JsonElement gameData) {
    this.gameData = gameData;
  }
}
