package network.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

  private final Map<String, ClientConnection> byUsername = new ConcurrentHashMap<>();

  /**
   * @return false when that account is already signed in somewhere else. Re-binding the same
   *     connection is allowed: it is the same session, not a second one.
   */
  public boolean bind(String username, ClientConnection connection) {
    ClientConnection existing = byUsername.putIfAbsent(username.toLowerCase(), connection);
    return existing == null || existing == connection;
  }

  public void rebind(String oldUsername, String newUsername, ClientConnection connection) {
    byUsername.remove(oldUsername.toLowerCase(), connection);
    byUsername.put(newUsername.toLowerCase(), connection);
  }

  public void unbind(ClientConnection connection) {
    byUsername.entrySet().removeIf(entry -> entry.getValue() == connection);
  }

  public boolean isOnline(String username) {
    return username != null && byUsername.containsKey(username.toLowerCase());
  }

  public ClientConnection connectionOf(String username) {
    return username == null ? null : byUsername.get(username.toLowerCase());
  }

  public Set<String> onlineUsers() {
    return Set.copyOf(byUsername.keySet());
  }

  public int count() {
    return byUsername.size();
  }
}
