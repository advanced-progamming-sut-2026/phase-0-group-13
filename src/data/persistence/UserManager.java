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
 * <p>The local file is still written, but only as a mirror. Nothing in register, login, password
 * recovery or profile retrieval reads it any more; it is there so the parts of Phase 1 that still
 * walk the local list (the offline leaderboard) keep working until they are moved across too.
 *
 * <p>The method signatures are unchanged on purpose, so the typed controllers and the LibGDX
 * screens call exactly what they called before.
 */
public class UserManager {
  private static final Gson GSON = new Gson();

  private record SavedSession(String username, String token) {}

  private static UserManager instance;
  private final List<User> users;
  private final JsonSerializer jsonSerializer;
  private final String usersFilePath;
  private User currentUser;
  private User pendingUser;
  // The server hashes the password itself, so the plain one has to survive until the account is
  // actually committed in setSecurityQuestionForLatestUser. It never leaves this object.
  private String pendingPassword;
  // The answer is only held so the reset call can send it again; the server decides if it is right.
  private String recoveryUsername;
  private String recoveryAnswer;
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
    if (stayLoggedIn && session.getAuthToken() != null) {
      saveSessionToDisk(user.getUsername(), session.getAuthToken());
    } else {
      this.isStayLoggedIn = false;
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
    // so the rest starts at the Phase 1 defaults and is pushed straight back up. A token login
    // has no password to hash.
    return new User(profile.username(), password == null ? "" : AuthService.hashPassword(password),
        "", profile.nickname(), "");
  }

  /** Everything about the signed-in player, in the shape the server stores. */
  private static Payloads.ProfileUpdate profileUpdateFor(User user) {
    return new Payloads.ProfileUpdate(
        user.getNickname(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getSecurityQuestionNumber(),
        user.getSecurityAnswer(),
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

  /** Step one of recovery: the server owns the lookup, the email match and the question. */
  public String initiatePasswordRecovery(String username, String email) throws Exception {
    ClientSession session = requireSession();
    Payloads.SecurityQuestionResponse response;
    try {
      response = session.requestSecurityQuestion(username, email);
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      this.recoveryUsername = null;
      this.recoveryAnswer = null;
      throw new Exception(response.message());
    }

    this.recoveryUsername = username;
    this.recoveryAnswer = null;

    model.enums.SecurityQuestion question =
            model.enums.SecurityQuestion.fromNumber(response.questionNumber());
    String questionText = question != null ? question.getText() : "(question unavailable)";

    return "Your security question is: " + questionText;
  }

  /** Step two. The server holds the answer, so it is the only thing that can check it. */
  public void verifyRecoveryAnswer(String answer) throws Exception {
    if (this.recoveryUsername == null) {
      throw new Exception("error: no password recovery session initiated");
    }
    ClientSession session = requireSession();
    Payloads.Ack response;
    try {
      response = session.resetPassword(this.recoveryUsername, answer, null);
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      this.recoveryUsername = null;
      this.recoveryAnswer = null;
      throw new Exception(response.message());
    }
    this.recoveryAnswer = answer;
    System.out.println("Answer verified successfully for user: " + this.recoveryUsername);
  }

  /** Step three. The new hash is written on the server; the local mirror is never the authority. */
  public void resetPasswordAfterRecovery(String newPassword) throws Exception {
    if (this.recoveryUsername == null || this.recoveryAnswer == null) {
      throw new Exception("error: no active recovery session. Please answer the security question first.");
    }

    Result passCheck = AuthService.checkPassword(newPassword);
    if (!passCheck.success()) throw new Exception(passCheck.message());

    ClientSession session = requireSession();
    Payloads.Ack response;
    try {
      response = session.resetPassword(this.recoveryUsername, this.recoveryAnswer, newPassword);
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      throw new Exception(response.message());
    }

    String username = this.recoveryUsername;
    // Keep the mirror in step so a stale hash cannot be read back by anything still using it.
    for (User cached : users) {
      if (cached.getUsername().equalsIgnoreCase(username)) {
        cached.setPasswordHash(AuthService.hashPassword(newPassword));
      }
    }
    saveUsersToJSON();

    System.out.println("Password reset successfully for user: " + username);
    this.recoveryUsername = null;
    this.recoveryAnswer = null;
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

  private void saveSessionToDisk(String username, String token) {
    jsonSerializer.writeToFile(getSessionFilePath(), new SavedSession(username, token));
  }

  private void clearSessionFromDisk() {
    java.io.File file = new java.io.File(getSessionFilePath());
    if (file.exists()) {
      file.delete();
    }
  }

  /**
   * Signs back in with the token the last "stay logged in" login saved. The server still
   * authenticates; a token it no longer recognises is dropped and the player signs in normally.
   */
  public boolean restoreSession() {
    // Called from both GameDataManager and the front end's start-up, so it must be safe to ask
    // twice: the second call must not go back to the server and must not drop the file.
    if (this.currentUser != null) {
      return this.isStayLoggedIn;
    }
    SavedSession saved =
            jsonSerializer.readFromFile(getSessionFilePath(), SavedSession.class);
    if (saved == null || saved.username() == null || saved.token() == null) {
      clearSessionFromDisk();
      return false;
    }

    ClientSession session = ClientSession.getInstance();
    if (!session.connect()) {
      // The server is down, not the token's fault; keep it for the next start-up.
      return false;
    }
    try {
      Payloads.AuthResponse response = session.loginWithToken(saved.username(), saved.token());
      if (!response.success()) {
        clearSessionFromDisk();
        return false;
      }
      this.currentUser = userFromProfile(response.profile(), null);
      seedQuests(this.currentUser);
      cacheLocally(this.currentUser);
      this.isStayLoggedIn = true;
      return true;
    } catch (IOException e) {
      return false;
    }
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

  /** Renames on the server first: a rename it refuses never reaches the player's own copy. */
  public void changeUsername(String newUsername) throws Exception {
    if (this.currentUser == null) throw new Exception("error: no user is currently logged in");
    Result usernameCheck = AuthService.checkUsername(newUsername);
    if (!usernameCheck.success()) throw new Exception(usernameCheck.message());
    if (this.currentUser.getUsername().equals(newUsername)) {
      throw new Exception("error: new username is the same as the current one");
    }

    String oldUsername = this.currentUser.getUsername();
    ClientSession session = requireSession();
    Payloads.Ack response;
    try {
      response = session.rename(newUsername);
    } catch (IOException e) {
      throw new Exception("error: could not reach the game server (" + e.getMessage() + ")");
    }
    if (!response.success()) {
      throw new Exception(response.message());
    }

    this.currentUser.setUsername(newUsername);
    users.removeIf(existing -> existing.getUsername().equalsIgnoreCase(oldUsername));
    pushCurrentUser();
    cacheLocally(this.currentUser);
    if (this.isStayLoggedIn && session.getAuthToken() != null) {
      saveSessionToDisk(newUsername, session.getAuthToken());
    }
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