package network.server;

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
    return success("registered", account);
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
    return success("logged in", account);
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
   * <p>Username and bestScore are deliberately left alone: the username is the store's key, and
   * bestScore is LeaderboardService's to move.
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
    account.setCoins(update.coins());
    account.setDiamonds(update.diamonds());
    if (update.gameData() != null) {
      account.setGameData(update.gameData());
    }
    store.save();
    return profileOf(account);
  }

  private static Payloads.AuthResponse success(String message, ServerAccount account) {
    return new Payloads.AuthResponse(true, message, profileOf(account));
  }

  private static Payloads.AuthResponse failure(String message) {
    return new Payloads.AuthResponse(false, message, null);
  }
}
