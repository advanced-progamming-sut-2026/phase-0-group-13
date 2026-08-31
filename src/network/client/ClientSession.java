package network.client;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieAction;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;

public final class ClientSession {

  public static final String HOST_PROPERTY = "pvz.server.host";
  public static final String PORT_PROPERTY = "pvz.server.port";

  private static final ClientSession INSTANCE = new ClientSession();

  private final NetworkClient client = new NetworkClient();
  private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();

  private volatile Payloads.Profile profile;
  private volatile Payloads.MatchFound match;
  private volatile String lastError;
  private volatile String authToken;
  private boolean listenerAttached;

  private ClientSession() {}

  public static ClientSession getInstance() {
    return INSTANCE;
  }

  public synchronized boolean connect() {
    if (client.isConnected()) {
      return true;
    }
    String host = System.getProperty(HOST_PROPERTY, NetworkClient.DEFAULT_HOST);
    int port = Integer.getInteger(PORT_PROPERTY, NetworkClient.DEFAULT_PORT);
    try {
      client.connect(host, port);
      if (!listenerAttached) {
        client.onEvent(this::onServerEvent);
        listenerAttached = true;
      }
      lastError = null;
      System.out.println("connected to the game server at " + host + ":" + port);
      return true;
    } catch (IOException e) {
      lastError = "error: cannot reach the game server at " + host + ":" + port
          + " (" + e.getMessage() + ")";
      System.out.println(lastError);
      return false;
    }
  }

  public boolean isConnected() {
    return client.isConnected();
  }

  public String getLastError() {
    return lastError == null ? "error: not connected to the game server" : lastError;
  }

  public Payloads.Profile getProfile() {
    return profile;
  }

  public String getUsername() {
    return profile == null ? null : profile.username();
  }

  public boolean isAuthenticated() {
    return profile != null;
  }

  public Payloads.MatchFound getMatch() {
    return match;
  }

  public MatchRole getRole() {
    Payloads.MatchFound current = match;
    return current == null ? null : current.role();
  }

  public void clearMatch() {
    match = null;
  }

  public Payloads.Ack requestMatchmaking(String game) throws IOException {
    return ack(client.request(MessageType.MATCHMAKING_REQUEST,
        new Payloads.MatchmakingRequest(game)));
  }

  public Payloads.Ack cancelMatchmaking() throws IOException {
    return ack(client.request(MessageType.MATCHMAKING_CANCEL, null));
  }

  public Payloads.Ack invite(String targetUsername) throws IOException {
    return ack(client.request(MessageType.MATCH_INVITE,
        new Payloads.MatchInviteRequest(targetUsername)));
  }

  public Payloads.Ack answerInvite(String inviteId, boolean accepted) throws IOException {
    return ack(client.request(MessageType.MATCH_INVITE_DECISION,
        new Payloads.MatchInviteDecision(inviteId, accepted)));
  }

  public String sendAction(String matchId, IZombieAction action) throws IOException {
    String kind = action.kind() == IZombieAction.Kind.PLACE_PLANT ? "place-plant" : "place-zombie";
    Payloads.Ack reply = ack(client.request(MessageType.GAME_ACTION,
        new Payloads.GameAction(matchId, kind, action.type(), action.row(), action.col())));
    return reply.success() ? null : reply.message();
  }

  public Payloads.Ack sendReaction(String matchId, Payloads.ReactionKind kind, String value)
      throws IOException {
    return ack(client.request(MessageType.REACTION, new Payloads.Reaction(matchId, kind, value)));
  }

  private static Payloads.Ack ack(NetworkMessage reply) {
    Payloads.Ack payload = reply.payloadAs(Payloads.Ack.class);
    if (payload == null) {
      return new Payloads.Ack(false, "error: empty response from the server");
    }
    return reply.type() == MessageType.ERROR && payload.success()
        ? new Payloads.Ack(false, payload.message())
        : payload;
  }

  public void onEvent(Consumer<NetworkMessage> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /** A screen that registered one must take it off again, or it keeps a disposed stage alive. */
  public void removeEventListener(Consumer<NetworkMessage> listener) {
    listeners.remove(listener);
  }

  private void onServerEvent(NetworkMessage message) {
    if (message.type() == MessageType.MATCH_FOUND) {
      match = message.payloadAs(Payloads.MatchFound.class);
    } else if (message.type() == MessageType.MATCH_ENDED) {
      match = null;
    }
    for (Consumer<NetworkMessage> listener : listeners) {
      listener.accept(message);
    }
  }

  public Payloads.AuthResponse register(Payloads.RegisterRequest request) throws IOException {
    NetworkMessage reply = client.request(MessageType.REGISTER_REQUEST, request);
    Payloads.AuthResponse response = reply.payloadAs(Payloads.AuthResponse.class);
    return response == null ? emptyAuthResponse() : response;
  }

  public Payloads.AuthResponse login(String username, String password) throws IOException {
    return accept(client.request(
        MessageType.LOGIN_REQUEST, new Payloads.LoginRequest(username, password)));
  }

  public Payloads.AuthResponse loginWithToken(String username, String token) throws IOException {
    return accept(client.request(
        MessageType.TOKEN_LOGIN_REQUEST, new Payloads.TokenLoginRequest(username, token)));
  }

  private Payloads.AuthResponse accept(NetworkMessage reply) {
    Payloads.AuthResponse response = reply.payloadAs(Payloads.AuthResponse.class);
    if (response == null) {
      return emptyAuthResponse();
    }
    if (response.success()) {
      profile = response.profile();
      authToken = response.token();
    }
    return response;
  }

  private static Payloads.AuthResponse emptyAuthResponse() {
    return new Payloads.AuthResponse(false, "error: empty response from the server", null, null);
  }

  public String getAuthToken() {
    return authToken;
  }

  public Payloads.SecurityQuestionResponse requestSecurityQuestion(String username, String email)
      throws IOException {
    NetworkMessage reply = client.request(MessageType.SECURITY_QUESTION_REQUEST,
        new Payloads.SecurityQuestionRequest(username, email));
    Payloads.SecurityQuestionResponse response =
        reply.payloadAs(Payloads.SecurityQuestionResponse.class);
    return response == null
        ? new Payloads.SecurityQuestionResponse(
            false, "error: empty response from the server", null)
        : response;
  }

  public Payloads.Ack resetPassword(String username, String answer, String newPassword)
      throws IOException {
    return ack(client.request(MessageType.PASSWORD_RESET,
        new Payloads.PasswordReset(username, answer, newPassword)));
  }

  public Payloads.Ack rename(String newUsername) throws IOException {
    Payloads.Ack result =
        ack(client.request(MessageType.RENAME_REQUEST, new Payloads.RenameRequest(newUsername)));
    if (result.success() && profile != null) {
      profile = new Payloads.Profile(newUsername, profile.nickname(), profile.email(),
          profile.gender(), profile.coins(), profile.diamonds(), profile.bestScore(),
          profile.gameData());
    }
    return result;
  }

  /**
   * Writes the player's data back to the server, which is where it lives now.
   *
   * @return the profile the server stored, so the caller can refresh from it
   */
  public Payloads.Profile pushProfile(Payloads.ProfileUpdate update) throws IOException {
    NetworkMessage reply = client.request(MessageType.PROFILE_UPDATE, update);
    Payloads.Profile stored = reply.payloadAs(Payloads.Profile.class);
    if (stored != null) {
      profile = stored;
    }
    return stored;
  }

  /**
   * The leaderboard as the server builds it. Nothing is cached: the doc wants the table to follow
   * the stored user data, so every open asks again rather than showing whatever was true last time.
   *
   * @param limit how many rows to ask for, or 0 for all of them
   */
  public Payloads.LeaderboardResponse requestLeaderboard(int limit) throws IOException {
    NetworkMessage reply =
        client.request(MessageType.LEADERBOARD_REQUEST, new Payloads.LeaderboardRequest(limit));
    failIfError(reply);
    Payloads.LeaderboardResponse response = reply.payloadAs(Payloads.LeaderboardResponse.class);
    return response == null || response.entries() == null
        ? new Payloads.LeaderboardResponse(List.of())
        : response;
  }

  public Payloads.ScoreResponse submitScore(int score) throws IOException {
    NetworkMessage reply =
        client.request(MessageType.SCORE_SUBMISSION, new Payloads.ScoreSubmission(score));
    failIfError(reply);
    Payloads.ScoreResponse response = reply.payloadAs(Payloads.ScoreResponse.class);
    return response == null ? new Payloads.ScoreResponse(false, null) : response;
  }

  private static void failIfError(NetworkMessage reply) throws IOException {
    if (reply.type() != MessageType.ERROR) {
      return;
    }
    Payloads.Ack ack = reply.payloadAs(Payloads.Ack.class);
    throw new IOException(ack == null || ack.message() == null
        ? "error: the server refused the request"
        : ack.message());
  }

  public JsonElement getGameData() {
    return profile == null ? null : profile.gameData();
  }

  public void logout() {
    profile = null;
    match = null;
    authToken = null;
    if (!client.isConnected()) {
      return;
    }
    try {
      client.request(MessageType.LOGOUT_REQUEST, null);
    } catch (IOException e) {
      System.out.println("logout could not reach the server: " + e.getMessage());
    }
  }

  public synchronized void disconnect() {
    profile = null;
    match = null;
    authToken = null;
    client.close();
  }
}
