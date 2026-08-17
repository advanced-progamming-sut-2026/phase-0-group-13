package network.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.enums.MatchRole;

public final class MatchService {

  private static final int DEFAULT_LEVEL = 1;

  private final Map<String, NetworkMatch> matches = new ConcurrentHashMap<>();

  public NetworkMatch create(String plantsPlayer, String zombiesPlayer) {
    String id = UUID.randomUUID().toString();
    NetworkMatch match =
        new NetworkMatch(id, plantsPlayer, zombiesPlayer, DEFAULT_LEVEL, System.nanoTime());
    matches.put(id, match);
    return match;
  }

  public NetworkMatch get(String matchId) {
    return matchId == null ? null : matches.get(matchId);
  }

  public NetworkMatch matchOf(String username) {
    for (NetworkMatch match : matches.values()) {
      if (!match.isFinished() && match.roleOf(username) != null) {
        return match;
      }
    }
    return null;
  }

  public MatchRole roleIn(String matchId, String username) {
    NetworkMatch match = get(matchId);
    return match == null ? null : match.roleOf(username);
  }

  public void end(String matchId) {
    NetworkMatch match = matches.remove(matchId);
    if (match != null) {
      match.markFinished();
    }
  }

  public int activeCount() {
    return matches.size();
  }
}
