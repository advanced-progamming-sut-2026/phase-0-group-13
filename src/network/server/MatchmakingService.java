package network.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MatchmakingService {

  private final Deque<String> queue = new ArrayDeque<>();
  private final Map<String, Invite> invites = new ConcurrentHashMap<>();

  public record Invite(String id, String from, String to) {}

  /** @return the waiting opponent to pair with, or null when the player now waits */
  public synchronized String enqueue(String username) {
    queue.remove(username);
    while (!queue.isEmpty()) {
      String opponent = queue.pollFirst();
      if (!opponent.equalsIgnoreCase(username)) {
        return opponent;
      }
    }
    queue.addLast(username);
    return null;
  }

  public synchronized void cancel(String username) {
    queue.remove(username);
    invites.entrySet().removeIf(
        entry -> entry.getValue().from().equalsIgnoreCase(username)
            || entry.getValue().to().equalsIgnoreCase(username));
  }

  public synchronized int queueSize() {
    return queue.size();
  }

  public synchronized boolean isQueued(String username) {
    for (String waiting : queue) {
      if (waiting.equalsIgnoreCase(username)) {
        return true;
      }
    }
    return false;
  }

  public Invite invite(String from, String to) {
    Invite invite = new Invite(UUID.randomUUID().toString(), from, to);
    invites.put(invite.id(), invite);
    return invite;
  }

  /**
   * Takes an invite, but only for the player it was addressed to.
   *
   * @return the invite, now consumed, or null if there is no such invite or it is not this
   * player's to accept
   */
  public synchronized Invite consumeInvite(String inviteId, String recipient) {
    Invite invite = inviteId == null ? null : invites.get(inviteId);
    if (invite == null || recipient == null || !invite.to().equalsIgnoreCase(recipient)) {
      return null;
    }
    invites.remove(inviteId);
    return invite;
  }
}
