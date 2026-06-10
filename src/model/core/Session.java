package model.core;

import model.account.User;

public class Session {
    private User currentUser;
    private boolean stayLoggedIn;
    private boolean authenticated;

    public Session() {
        this.currentUser = null;
        this.stayLoggedIn = false;
        this.authenticated = false;
    }

    public void login() {
        authenticated = true;
    }

    public void logout() {
        authenticated = false;
        currentUser = null;
    }

    public void restoreSession() {
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
