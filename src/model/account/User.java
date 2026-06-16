package model.account;

public class User {
    private String username;
    private String passwordHash;
    private String email;
    private PlayerProfile playerProfile;
    private String securityQuestionNumber;
    private String securityAnswer;

    public User() {}

    public User(String username, String passwordHash, String email, String nickname, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.playerProfile = new PlayerProfile(nickname, gender, null);
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSecurityQuestionNumber() { return securityQuestionNumber; }
    public void setSecurityQuestion(String number, String answer) {
        this.securityQuestionNumber = number;
        this.securityAnswer = answer;
    }
    public String getSecurityAnswer() { return securityAnswer; }
}