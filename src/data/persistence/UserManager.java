package data.persistence;

import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import model.account.User;
import model.core.AuthService;
import model.Result;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    private final List<User> users;
    private final JsonSerializer jsonSerializer;
    private final String usersFilePath;
    private User currentUser;
    private User recoveryUser;
    private boolean isStayLoggedIn = false;

    private UserManager() {
        this.jsonSerializer = new JsonSerializer();
        Path path = DataPath.getInstance().getPath("users");
        this.usersFilePath = path != null ? path.toString() : "data/database/Users.json";

        this.users = loadUsersFromJSON();
    }

    public static UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void registerUser(String username, String password, String nickname, String email, String gender) throws Exception {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                throw new Exception("error: username already exists");
            }
        }

        Result passCheck = AuthService.checkPassword(password);
        if (!passCheck.success()) throw new Exception(passCheck.message());

        Result emailCheck = AuthService.checkEmail(email);
        if (!emailCheck.success()) throw new Exception(emailCheck.message());

        String hashedPass = AuthService.hashPassword(password);
        User newUser = new User(username, hashedPass, email, nickname, gender);

        users.add(newUser);

        saveUsersToJSON();
    }

    public void loginUser(String username, String password, boolean stayLoggedIn) throws Exception {
        User foundUser = null;
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                foundUser = u;
                break;
            }
        }

        if (foundUser == null) {
            throw new Exception("error: username not found");
        }

        String inputHash = AuthService.hashPassword(password);
        if (!foundUser.getPasswordHash().equals(inputHash)) {
            throw new Exception("error: incorrect password");
        }

        this.currentUser = foundUser;
        this.isStayLoggedIn = stayLoggedIn;
        if (stayLoggedIn) {
            saveSessionToDisk(username);
        } else {
            clearSessionFromDisk();
        }

    }

    private void saveUsersToJSON() {
        jsonSerializer.writeToFile(usersFilePath, this.users);
    }

    private List<User> loadUsersFromJSON() {
        User[] loadedArray = jsonSerializer.readFromFile(usersFilePath, User[].class);
        if (loadedArray == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(loadedArray));
    }
    public void setSecurityQuestionForLatestUser(String qNumber, String answer) throws Exception {
        if (users.isEmpty()) throw new Exception("error: no user registered yet");

        User latestUser = users.get(users.size() - 1);
        latestUser.setSecurityQuestion(qNumber, answer);
        saveUsersToJSON();
    }

    public String initiatePasswordRecovery(String username, String email) throws Exception {
        User foundUser = null;
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                foundUser = u;
                break;
            }
        }

        if (foundUser == null) {
            throw new Exception("error: username not found");
        }

        if (!foundUser.getEmail().equals(email)) {
            throw new Exception("error: email does not match");
        }

        this.recoveryUser = foundUser;

        return "Your security question is number " + foundUser.getSecurityQuestionNumber() + ". Please answer it:";
    }

    public void verifyRecoveryAnswer(String answer) throws Exception {
        if (this.recoveryUser == null) {
            throw new Exception("error: no password recovery session initiated");
        }

        if (!this.recoveryUser.getSecurityAnswer().equals(answer)) {
            throw new Exception("error: incorrect security answer");
        }
        System.out.println("Answer verified successfully for user: " + recoveryUser.getUsername());
        this.recoveryUser = null;
    }
    public void logout() {
        this.currentUser = null;
        this.isStayLoggedIn = false;
        clearSessionFromDisk();
    }
    private void saveSessionToDisk(String username) {
        String sessionPath = "data/database/session.json";
        jsonSerializer.writeToFile(sessionPath, username);
    }

    private void clearSessionFromDisk() {
        java.io.File file = new java.io.File("data/database/session.json");
        if (file.exists()) {
            file.delete();
        }
    }
}