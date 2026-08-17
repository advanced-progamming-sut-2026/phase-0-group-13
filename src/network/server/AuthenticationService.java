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
        account.getBestScore());
  }

  private static Payloads.AuthResponse success(String message, ServerAccount account) {
    return new Payloads.AuthResponse(true, message, profileOf(account));
  }

  private static Payloads.AuthResponse failure(String message) {
    return new Payloads.AuthResponse(false, message, null);
  }
}
