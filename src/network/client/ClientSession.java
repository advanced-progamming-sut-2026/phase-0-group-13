package network.client;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.Payloads;

/**
 * The one connection the client has to the server, shared by every screen and controller.
 *
 * <p>{@link NetworkClient} is the transport and knows nothing about the game; this is the layer
 * above it that the rest of the client talks to. It exists so there is exactly one socket per
 * running client: screens come and go, and a screen that opened its own NetworkClient would leave
 * the server with a dangling session and lose every event pushed to the old one.
 *
 * <p>What it owns:
 *
 * <ul>
 *   <li>the connected {@link NetworkClient};
 *   <li>the {@link Payloads.Profile} the server returned at login, which is the authoritative copy
 *       of the account;
 *   <li>the current {@link Payloads.MatchFound}, kept up to date from MATCH_FOUND / MATCH_ENDED
 *       because those are the only match facts the protocol carries today;
 *   <li>event fan-out, so several listeners can watch server pushes without fighting over
 *       NetworkClient's single listener list.
 * </ul>
 *
 * <p>The host and port come from {@code -Dpvz.server.host} / {@code -Dpvz.server.port} so a second
 * client can be pointed somewhere else, same convention as {@code -Dpvz.debug}.
 */
public final class ClientSession {

  public static final String HOST_PROPERTY = "pvz.server.host";
  public static final String PORT_PROPERTY = "pvz.server.port";

  private static final ClientSession INSTANCE = new ClientSession();

  private final NetworkClient client = new NetworkClient();
  private final List<Consumer<NetworkMessage>> listeners = new CopyOnWriteArrayList<>();

  private volatile Payloads.Profile profile;
  private volatile Payloads.MatchFound match;
  private volatile String lastError;
  private boolean listenerAttached;

  private ClientSession() {}

  public static ClientSession getInstance() {
    return INSTANCE;
  }

  /**
   * Opens the connection if it is not already open. Safe to call repeatedly: the launchers call it
   * once at start-up and the account layer calls it again before anything that needs the server, so
   * a client started before the server can still recover without a restart.
   *
   * @return true when there is a usable connection
   */
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

  /** Why the last connect() failed, for callers that turn it into a user-facing message. */
  public String getLastError() {
    return lastError == null ? "error: not connected to the game server" : lastError;
  }

  /** The account as the server last described it, or null when nobody is signed in. */
  public Payloads.Profile getProfile() {
    return profile;
  }

  public String getUsername() {
    return profile == null ? null : profile.username();
  }

  public boolean isAuthenticated() {
    return profile != null;
  }

  /** The match this client is in, or null. Set from MATCH_FOUND, cleared on MATCH_ENDED. */
  public Payloads.MatchFound getMatch() {
    return match;
  }

  /** Server pushes (events, not replies) go to every listener registered here. */
  public void onEvent(Consumer<NetworkMessage> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
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

  /** Creates the account on the server. The server owns the username-uniqueness rule. */
  public Payloads.AuthResponse register(Payloads.RegisterRequest request) throws IOException {
    NetworkMessage reply = client.request(MessageType.REGISTER_REQUEST, request);
    Payloads.AuthResponse response = reply.payloadAs(Payloads.AuthResponse.class);
    return response == null
        ? new Payloads.AuthResponse(false, "error: empty response from the server", null)
        : response;
  }

  /** Signs in against the server and keeps the returned profile as the authoritative account. */
  public Payloads.AuthResponse login(String username, String password) throws IOException {
    NetworkMessage reply =
        client.request(MessageType.LOGIN_REQUEST, new Payloads.LoginRequest(username, password));
    Payloads.AuthResponse response = reply.payloadAs(Payloads.AuthResponse.class);
    if (response == null) {
      return new Payloads.AuthResponse(false, "error: empty response from the server", null);
    }
    if (response.success()) {
      profile = response.profile();
    }
    return response;
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

  /**
   * Sends a bonus-game run's My-Point to the server, which keeps it only if it beats the record.
   *
   * <p>This is the only way the leaderboard's My Point column ever gets a value: the server refuses
   * to take it from PROFILE_UPDATE, so a player who has not finished a bonus run keeps an empty
   * column instead of a zero.
   */
  public Payloads.ScoreResponse submitScore(int score) throws IOException {
    NetworkMessage reply =
        client.request(MessageType.SCORE_SUBMISSION, new Payloads.ScoreSubmission(score));
    failIfError(reply);
    Payloads.ScoreResponse response = reply.payloadAs(Payloads.ScoreResponse.class);
    return response == null ? new Payloads.ScoreResponse(false, null) : response;
  }

  /**
   * Turns the server's ERROR reply into a thrown failure.
   *
   * <p>Without this the caller reads an error reply as the payload it asked for, and an ERROR
   * carries none of those fields, so every one comes back null or zero. "You are not logged in"
   * then arrives at a screen as an empty leaderboard, which is a different and much more
   * confusing thing to be told.
   */
  private static void failIfError(NetworkMessage reply) throws IOException {
    if (reply.type() != MessageType.ERROR) {
      return;
    }
    Payloads.Ack ack = reply.payloadAs(Payloads.Ack.class);
    throw new IOException(ack == null || ack.message() == null
        ? "error: the server refused the request"
        : ack.message());
  }

  /** The player's data as the server holds it, or null when it has none for this account yet. */
  public JsonElement getGameData() {
    return profile == null ? null : profile.gameData();
  }

  /**
   * Releases the server-side session so the same account can sign in again from another client.
   * Failures are swallowed: dropping the connection unbinds the session anyway.
   */
  public void logout() {
    profile = null;
    match = null;
    if (!client.isConnected()) {
      return;
    }
    try {
      client.request(MessageType.LOGOUT_REQUEST, null);
    } catch (IOException e) {
      System.out.println("logout could not reach the server: " + e.getMessage());
    }
  }

  /** Closes the socket. Only the launchers should need this. */
  public synchronized void disconnect() {
    profile = null;
    match = null;
    client.close();
  }
}
