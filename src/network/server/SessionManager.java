package network.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

  private final Map<String, ClientConnection> byUsername = new ConcurrentHashMap<>();

  /** @return false when that account is already signed in somewhere else */
  public boolean bind(String username, ClientConnection connection) {
    return byUsername.putIfAbsent(username.toLowerCase(), connection) == null;
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
