package data.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Result;
import model.account.User;
import model.core.AuthService;
import network.client.ClientSession;
import network.protocol.Payloads;

/**
 * The account layer. Since Phase 3 the server is the authority: registration, login and the
 * player's data all go through {@link ClientSession}, and nothing here decides who exists or
 * whether a password is right.
 *
 * <p>The local file is still written, but only as a mirror. Nothing in register, login or profile
 * retrieval reads it any more; it is there so the parts of Phase 1 that still walk the local list
 * (the offline leaderboard, password recovery) keep working until they are moved across too.
 *
 * <p>The method signatures are unchanged on purpose, so the typed controllers and the LibGDX
 * screens call exactly what they called before.
 */
public class UserManager {
  private static final Gson GSON = new Gson();

  private static UserManager instance;
  private final List<User> users;
  private final JsonSerializer jsonSerializer;
  private final String usersFilePath;
  private User currentUser;
  private User recoveryUser;
  private User pendingUser;
  // The server hashes the password itself, so the plain one has to survive until the account is
  // actually committed in setSecurityQuestionForLatestUser. It never leaves this object.
  private String pendingPassword;
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
    Result usernameCheck = AuthService.checkUsername(username);
    if (!usernameCheck.success()) throw new Exception(usernameCheck.message());

    // No local duplicate scan any more: the server keeps the account list, so it is the only place
    // that can answer whether a username is taken. It reports it at the commit step below.
    Result passCheck = AuthService.checkPassword(password);
    if (!passCheck.success()) throw new Exception(passCheck.message());

    Result emailCheck = AuthService.checkEmail(email);
    if (!emailCheck.success()) throw new Exception(emailCheck.message());

    Result nicknameCheck = AuthService.checkNickname(nickname);
    if (!nicknameCheck.success()) throw new Exception(nicknameCheck.message());

    Result genderCheck = AuthService.checkGender(gender);
    if (!genderCheck.success()) throw new Exception(genderCheck.message());

    String hashedPass = AuthService.hashPassword(password);

    this.pendingUser = new User(username, hashedPass, email, nickname, gender);
    this.pendingPassword = password;
  }

  /**
   * Signs in against the server. The username lookup and the password comparison happen there, so
   * the same account works from any machine; the messages thrown are the server's own.
   */
  public void loginUser(String username, String password, boolean stayLoggedIn) throws Exception {
    ClientSession session = requireSession();
    Payloads.AuthResponse response;
    try {
      response = session.login(username, password);
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      throw new Exception(response.message());
    }

    User user = userFromProfile(response.profile(), password);
    this.currentUser = user;
    seedQuests(user);
    if (response.profile().gameData() == null) {
      // First sign-in of an account the server has no document for; hand it the defaults so it
      // becomes the authority from here on.
      pushCurrentUser();
    }
    cacheLocally(user);

    this.isStayLoggedIn = stayLoggedIn;
    if (stayLoggedIn) {
      saveSessionToDisk(username);
    } else {
      clearSessionFromDisk();
    }
  }

  private ClientSession requireSession() throws Exception {
    ClientSession session = ClientSession.getInstance();
    if (!session.connect()) {
      throw new Exception(session.getLastError());
    }
    return session;
  }

  /** Rebuilds the player from what the server sent back. */
  private static User userFromProfile(Payloads.Profile profile, String password) {
    JsonElement data = profile.gameData();
    if (data != null && data.isJsonObject()) {
      User restored = GSON.fromJson(data, User.class);
      if (restored != null) {
        return restored;
      }
    }
    // An account the server holds no document for yet. It only knows the username and nickname,
    // so the rest starts at the Phase 1 defaults and is pushed straight back up.
    return new User(profile.username(), AuthService.hashPassword(password), "",
        profile.nickname(), "");
  }

  /** Everything about the signed-in player, in the shape the server stores. */
  private static Payloads.ProfileUpdate profileUpdateFor(User user) {
    return new Payloads.ProfileUpdate(
        user.getNickname(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getCoins(),
        user.getDiamonds(),
        GSON.toJsonTree(user));
  }

  /** Writes the signed-in player back to the server, which is where the account lives. */
  private void pushCurrentUser() throws Exception {
    ClientSession session = requireSession();
    try {
      session.pushProfile(profileUpdateFor(this.currentUser));
    } catch (IOException e) {
      throw new Exception("error: could not save your account to the server (" + e.getMessage() + ")");
    }
  }

  /** Mirror only - see the class comment. Never read back for authentication. */
  private void cacheLocally(User user) {
    users.removeIf(existing -> existing.getUsername().equalsIgnoreCase(user.getUsername()));
    users.add(user);
    saveUsersToJSON();
  }

  private void saveUsersToJSON() {
    jsonSerializer.writeToFile(usersFilePath, this.users);
  }

  private List<User> loadUsersFromJSON() {
    User[] loadedArray = jsonSerializer.readFromFile(usersFilePath, User[].class);
    if (loadedArray == null) return new ArrayList<>();
    return new ArrayList<>(Arrays.asList(loadedArray));
  }

  /**
   * Commits the registration. Phase 1 only counted an account as real once the security question
   * was picked, so this is where the server is asked to create it - and where it answers whether
   * the username was free.
   */
  public void setSecurityQuestionForLatestUser(String qNumber, String answer) throws Exception {
    if (this.pendingUser == null) {
      throw new Exception("error: no pending registration - please register first");
    }

    this.pendingUser.setSecurityQuestion(qNumber, answer);

    ClientSession session = requireSession();
    Payloads.AuthResponse response;
    try {
      response = session.register(new Payloads.RegisterRequest(
          pendingUser.getUsername(),
          pendingPassword,
          pendingUser.getNickname(),
          pendingUser.getEmail(),
          pendingUser.getGender()));
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      throw new Exception(response.message());
    }

    // The account exists on the server now; give it the player's document too (security question
    // included) so nothing about the account is left behind on this machine.
    try {
      session.login(pendingUser.getUsername(), pendingPassword);
      session.pushProfile(profileUpdateFor(pendingUser));
      session.logout();
    } catch (IOException e) {
      throw new Exception("error: the account was created but its data could not be saved ("
          + e.getMessage() + ")");
    }

    cacheLocally(this.pendingUser);
    this.pendingUser = null;
    this.pendingPassword = null;
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

    model.enums.SecurityQuestion question =
            model.enums.SecurityQuestion.fromNumber(foundUser.getSecurityQuestionNumber());
    String questionText = question != null ? question.getText() : "(question unavailable)";

    return "Your security question is: " + questionText;
  }

  public void verifyRecoveryAnswer(String answer) throws Exception {
    if (this.recoveryUser == null) {
      throw new Exception("error: no password recovery session initiated");
    }

    if (!this.recoveryUser.getSecurityAnswer().equals(answer)) {
      this.recoveryUser = null;
      throw new Exception("error: incorrect security answer");
    }
    System.out.println("Answer verified successfully for user: " + recoveryUser.getUsername());
  }

  public void resetPasswordAfterRecovery(String newPassword) throws Exception {
    if (this.recoveryUser == null) {
      throw new Exception("error: no active recovery session. Please answer the security question first.");
    }

    Result passCheck = AuthService.checkPassword(newPassword);
    if (!passCheck.success()) throw new Exception(passCheck.message());

    this.recoveryUser.setPasswordHash(AuthService.hashPassword(newPassword));
    saveUsersToJSON();

    System.out.println("Password reset successfully for user: " + this.recoveryUser.getUsername());
    this.recoveryUser = null;
  }

  public void logout() {
    this.currentUser = null;
    this.isStayLoggedIn = false;
    // Let the server drop the session, otherwise this account stays bound to the old connection
    // and cannot sign in again until the socket closes.
    ClientSession.getInstance().logout();
    model.core.MatchSetup.reset();
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

  /**
   * Always false now that the server authenticates.
   *
   * <p>The saved session only holds a username, and LOGIN_REQUEST needs the password the server
   * hashes itself, so there is nothing here to sign in with. Restoring the locally cached account
   * instead would put the authority back on this machine, which is exactly what Phase 3 moves
   * away from, so the stale file is dropped and the player signs in against the server.
   */
  public boolean restoreSession() {
    clearSessionFromDisk();
    return false;
  }

  private void seedQuests(User user) {
    if (user == null || data.GameDataManager.questRepository == null) {
      return;
    }
    user.seedQuestsIfNeeded(data.GameDataManager.questRepository.getAll());
  }

  public boolean isStayLoggedIn() {
    return isStayLoggedIn;
  }

  public void updateCurrentUserGameState() throws Exception {
    if (this.currentUser == null) {
      throw new Exception("error: no user is currently logged in to save game state.");
    }
    pushCurrentUser();
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
    Result usernameCheck = AuthService.checkUsername(newUsername);
    if (!usernameCheck.success()) throw new Exception(usernameCheck.message());
    if (this.currentUser.getUsername().equals(newUsername)) {
      throw new Exception("error: new username is the same as the current one");
    }
    for (User u : users) {
      if (u.getUsername().equals(newUsername))
        throw new Exception("error: username already exists");
    }
    // Not pushed: the server keys accounts by username and the protocol has no rename request,
    // so a rename only lives on this machine and the server's name wins at the next sign-in.
    this.currentUser.setUsername(newUsername);
    saveUsersToJSON();
  }

  public void changeNickname(String newNickname) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    Result nicknameCheck = AuthService.checkNickname(newNickname);
    if (!nicknameCheck.success()) throw new Exception(nicknameCheck.message());
    if (this.currentUser.getNickname().equals(newNickname)) {
      throw new Exception("error: new nickname is the same as the current one");
    }
    this.currentUser.setNickname(newNickname);
    pushCurrentUser();
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
    pushCurrentUser();
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
    pushCurrentUser();
    saveUsersToJSON();
  }
}