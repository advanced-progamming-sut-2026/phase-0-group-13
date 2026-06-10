package model.account;

public class User {
    private String username;
    private String passwordHash;
    private String email;

    public User() {
    }

    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public void register() {
    }

    public void login() {
    }

    public void changePassword() {
    }
}
