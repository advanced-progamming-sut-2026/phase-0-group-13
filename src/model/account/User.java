package model.account;

public class User {
    private String username;
    private String passwordHash;
    private String email;
    private PlayerProfile playerProfile;
    private Progress progress;
    private Collection collection;
    public User() {
        // داخل این میایم کالکشنو پروفایل و ... رو اینیشالایز میکنیم
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
