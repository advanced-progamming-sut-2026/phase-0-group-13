package model.core;

import model.account.User;

public class Session {
    // با الگوی سینگلتون رفتار میکنیم
    private static Session instance;
    private User loggedInUser;

    private Session() {
        this.loggedInUser = null;
    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void login(User user) {
        this.loggedInUser = user;
    }

    public void logout() {
        this.loggedInUser = null;
    }

    public boolean isLoggedIn() {
        return this.loggedInUser != null;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
}