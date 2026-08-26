package network.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import model.Result;
import model.core.AuthService;
import network.protocol.Payloads;

/** Reuses AuthService so the server checks the same rules as the offline game. */
public final class AuthenticationService {

  private final ServerAccountStore store;

  public AuthenticationService(ServerAccountStore store) {
    this.store = store;
  }

  public Payloads.AuthResponse register(Payloads.RegisterRequest request) {
    if (request == null) {
      return failure("error: empty request");
    }
    Result[] checks = {
      AuthService.checkUsername(request.username()),
      AuthService.checkPassword(request.password()),
      AuthService.checkNickname(request.nickname()),
      AuthService.checkEmail(request.email()),
      AuthService.checkGender(request.gender())
    };
    for (Result check : checks) {
      if (!check.success()) {
        return failure(check.message());
      }
    }
    if (store.find(request.username()) != null) {
      return failure("error: username already exists");
    }

    ServerAccount account = new ServerAccount(
        request.username(),
        AuthService.hashPassword(request.password()),
        request.nickname(),
        request.email(),
        request.gender());
    store.add(account);
    return success("registered", account, null);
  }

  public Payloads.AuthResponse login(Payloads.LoginRequest request) {
    if (request == null) {
      return failure("error: empty request");
    }
    ServerAccount account = store.find(request.username());
    if (account == null) {
      return failure("error: username not found");
    }
    if (!account.getPasswordHash().equals(AuthService.hashPassword(request.password()))) {
      return failure("error: incorrect password");
    }
    return success("logged in", account, issueToken(account));
  }

  /** The token is the whole credential; a stale one is refused like a wrong password. */
  public Payloads.AuthResponse loginWithToken(Payloads.TokenLoginRequest request) {
    if (request == null || request.token() == null) {
      return failure("error: empty request");
    }
    ServerAccount account = store.find(request.username());
    if (account == null || account.getSessionToken() == null
        || !account.getSessionToken().equals(request.token())) {
      return failure("error: your saved session is no longer valid - please log in");
    }
    return success("logged in", account, account.getSessionToken());
  }

  public void clearToken(String username) {
    ServerAccount account = store.find(username);
    if (account != null && account.getSessionToken() != null) {
      account.setSessionToken(null);
      store.save();
    }
  }

  private String issueToken(ServerAccount account) {
    account.setSessionToken(UUID.randomUUID().toString());
    store.save();
    return account.getSessionToken();
  }

  /** The email must match too, or the questions would be enumerable by username alone. */
  public Payloads.SecurityQuestionResponse securityQuestion(Payloads.SecurityQuestionRequest req) {
    if (req == null) {
      return new Payloads.SecurityQuestionResponse(false, "error: empty request", null);
    }
    ServerAccount account = store.find(req.username());
    if (account == null) {
      return new Payloads.SecurityQuestionResponse(false, "error: username not found", null);
    }
    String email = account.getEmail() == null ? stringFromGameData(account, "email")
        : account.getEmail();
    if (email == null || !email.equalsIgnoreCase(req.email())) {
      return new Payloads.SecurityQuestionResponse(false, "error: email does not match", null);
    }
    String question = questionNumberOf(account);
    if (question == null) {
      return new Payloads.SecurityQuestionResponse(
          false, "error: this account has no security question on file", null);
    }
    return new Payloads.SecurityQuestionResponse(true, "ok", question);
  }

  /**
   * Checks the answer and, when a new password came with it, rewrites the hash.
   *
   * <p>The stored player document carries its own copy of the hash, so that is rewritten too, or
   * the next login would hand back an account whose password field disagrees with it.
   */
  public Payloads.Ack resetPassword(Payloads.PasswordReset request) {
    if (request == null) {
      return new Payloads.Ack(false, "error: empty request");
    }
    ServerAccount account = store.find(request.username());
    if (account == null) {
      return new Payloads.Ack(false, "error: username not found");
    }
    String answer = answerOf(account);
    if (answer == null) {
      return new Payloads.Ack(false, "error: this account has no security question on file");
    }
    if (request.answer() == null || !answer.equals(request.answer().trim())) {
      return new Payloads.Ack(false, "error: incorrect security answer");
    }
    if (request.newPassword() == null) {
      return new Payloads.Ack(true, "answer accepted");
    }
    Result check = AuthService.checkPassword(request.newPassword());
    if (!check.success()) {
      return new Payloads.Ack(false, check.message());
    }

    String hash = AuthService.hashPassword(request.newPassword());
    account.setPasswordHash(hash);
    account.setSessionToken(null);
    JsonObject data = gameData(account);
    if (data != null) {
      data.addProperty("passwordHash", hash);
    }
    store.save();
    return new Payloads.Ack(true, "password reset");
  }

  /** Accounts are found by username, not keyed by it, so a rename is the field plus the copy. */
  public Payloads.Ack rename(String currentUsername, String newUsername) {
    ServerAccount account = store.find(currentUsername);
    if (account == null) {
      return new Payloads.Ack(false, "error: username not found");
    }
    if (newUsername == null || newUsername.equalsIgnoreCase(currentUsername)) {
      return new Payloads.Ack(false, "error: new username is the same as the current one");
    }
    Result check = AuthService.checkUsername(newUsername);
    if (!check.success()) {
      return new Payloads.Ack(false, check.message());
    }
    if (store.find(newUsername) != null) {
      return new Payloads.Ack(false, "error: username already exists");
    }

    account.setUsername(newUsername);
    JsonObject data = gameData(account);
    if (data != null) {
      data.addProperty("username", newUsername);
    }
    store.save();
    return new Payloads.Ack(true, "username changed");
  }

  public static Payloads.Profile profileOf(ServerAccount account) {
    return new Payloads.Profile(
        account.getUsername(),
        account.getNickname(),
        account.getCoins(),
        account.getDiamonds(),
        account.getBestScore(),
        account.getGameData());
  }

  /**
   * Stores what the signed-in client sent for its own account and returns what is now on file.
   *
   * <p>Username and bestScore are deliberately left alone: the username moves only through
   * {@link #rename}, and bestScore is LeaderboardService's to move.
   *
   * @return the stored profile, or null when there is no such account
   */
  public Payloads.Profile update(String username, Payloads.ProfileUpdate update) {
    ServerAccount account = store.find(username);
    if (account == null || update == null) {
      return null;
    }
    if (update.nickname() != null) {
      account.setNickname(update.nickname());
    }
    if (update.email() != null) {
      account.setEmail(update.email());
    }
    if (update.passwordHash() != null) {
      account.setPasswordHash(update.passwordHash());
    }
    if (update.securityQuestionNumber() != null) {
      account.setSecurityQuestionNumber(update.securityQuestionNumber());
    }
    if (update.securityAnswer() != null) {
      account.setSecurityAnswer(update.securityAnswer());
    }
    account.setCoins(update.coins());
    account.setDiamonds(update.diamonds());
    if (update.gameData() != null) {
      account.setGameData(update.gameData());
    }
    store.save();
    return profileOf(account);
  }

  /**
   * The security question and answer became fields on the account only once recovery moved to the
   * server; accounts stored before that carry them inside the player document, so both are read
   * from the field first and from the document as a fallback.
   */
  private static String questionNumberOf(ServerAccount account) {
    return account.getSecurityQuestionNumber() != null
        ? account.getSecurityQuestionNumber()
        : stringFromGameData(account, "securityQuestionNumber");
  }

  private static String answerOf(ServerAccount account) {
    return account.getSecurityAnswer() != null
        ? account.getSecurityAnswer()
        : stringFromGameData(account, "securityAnswer");
  }

  private static JsonObject gameData(ServerAccount account) {
    JsonElement data = account.getGameData();
    return data != null && data.isJsonObject() ? data.getAsJsonObject() : null;
  }

  private static String stringFromGameData(ServerAccount account, String field) {
    JsonObject data = gameData(account);
    JsonElement value = data == null ? null : data.get(field);
    return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
  }

  private static Payloads.AuthResponse success(
      String message, ServerAccount account, String token) {
    return new Payloads.AuthResponse(true, message, profileOf(account), token);
  }

  private static Payloads.AuthResponse failure(String message) {
    return new Payloads.AuthResponse(false, message, null, null);
  }
}
