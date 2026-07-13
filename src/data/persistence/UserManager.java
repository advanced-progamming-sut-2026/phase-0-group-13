package data.persistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Result;
import model.account.User;
import model.core.AuthService;

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

  public void registerUser(
          String username, String password, String nickname, String email, String gender)
          throws Exception {
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

    return "Your security question is number "
            + foundUser.getSecurityQuestionNumber()
            + ". Please answer it:";
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

  private String getSessionFilePath() {
    Path path = DataPath.getInstance().getPath("session");
    return path != null ? path.toString() : "data/database/session.json";
  }

  private void saveSessionToDisk(String username) {
    jsonSerializer.writeToFile(getSessionFilePath(), username);
  }

  private void clearSessionFromDisk() {
    java.io.File file = new java.io.File(getSessionFilePath());
    if (file.exists()) {
      file.delete();
    }
  }


  public boolean restoreSession() {
    String savedUsername = jsonSerializer.readFromFile(getSessionFilePath(), String.class);
    if (savedUsername == null || savedUsername.isEmpty()) {
      return false;
    }

    for (User u : users) {
      if (u.getUsername().equals(savedUsername)) {
        this.currentUser = u;
        this.isStayLoggedIn = true;
        return true;
      }
    }

    clearSessionFromDisk();
    return false;
  }

  public boolean isStayLoggedIn() {
    return isStayLoggedIn;
  }

  public void updateCurrentUserGameState() throws Exception {
    if (this.currentUser == null) {
      throw new Exception("error: no user is currently logged in to save game state.");
    }
    saveUsersToJSON();
  }

  public User getCurrentUser() {
    return this.currentUser;
  }

  public List<User> getAllUsers() {
    return this.users;
  }

  public void changeUsername(String newUsername) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    if (this.currentUser.getUsername().equals(newUsername)) {
      throw new Exception("error: new username is the same as the current one");
    }
    for (User u : users) {
      if (u.getUsername().equals(newUsername))
        throw new Exception("error: username already exists");
    }
    this.currentUser.setUsername(newUsername);
    saveUsersToJSON();
  }

  public void changeNickname(String newNickname) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    if (this.currentUser.getNickname().equals(newNickname)) {
      throw new Exception("error: new nickname is the same as the current one");
    }
    this.currentUser.setNickname(newNickname);
    saveUsersToJSON();
  }

  public void changeEmail(String newEmail) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    if (this.currentUser.getEmail().equals(newEmail)) {
      throw new Exception("error: new email is the same as the current one");
    }
    Result emailCheck = AuthService.checkEmail(newEmail);
    if (!emailCheck.success()) throw new Exception(emailCheck.message());
    this.currentUser.setEmail(newEmail);
    saveUsersToJSON();
  }

  public void changePassword(String newPassword, String oldPassword) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    String oldHash = AuthService.hashPassword(oldPassword);
    if (!this.currentUser.getPasswordHash().equals(oldHash)) {
      throw new Exception("error: old password is incorrect");
    }
    if (this.currentUser.getPasswordHash().equals(AuthService.hashPassword(newPassword))) {
      throw new Exception("error: new password is the same as the current one");
    }
    Result passCheck = AuthService.checkPassword(newPassword);
    if (!passCheck.success()) throw new Exception(passCheck.message());
    this.currentUser.setPasswordHash(AuthService.hashPassword(newPassword));
    saveUsersToJSON();
  }
}