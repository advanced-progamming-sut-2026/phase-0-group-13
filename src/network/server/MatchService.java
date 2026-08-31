package network.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieMatch;

public final class MatchService {

  private static final int DEFAULT_LEVEL = 1;

  /** Long enough for any tick to finish, short enough that shutdown can never hang the server. */
  private static final long SHUTDOWN_WAIT_MILLIS = 2000;

  public interface Listener {
    void onTick(NetworkMatch match);

    void onFinished(NetworkMatch match);
  }

  private final Map<String, NetworkMatch> matches = new ConcurrentHashMap<>();

  private volatile Listener listener;
  private ScheduledExecutorService scheduler;

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public NetworkMatch create(String plantsPlayer, String zombiesPlayer) {
    String id = UUID.randomUUID().toString();
    NetworkMatch match =
        new NetworkMatch(id, plantsPlayer, zombiesPlayer, DEFAULT_LEVEL, System.nanoTime());
    matches.put(id, match);
    return match;
  }

  public synchronized void start() {
    if (scheduler != null) {
      return;
    }
    scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "match-clock");
      thread.setDaemon(true);
      return thread;
    });
    scheduler.scheduleAtFixedRate(this::tickAll, IZombieMatch.TICK_MILLIS,
        IZombieMatch.TICK_MILLIS, TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    ScheduledExecutorService stopping;
    synchronized (this) {
      if (scheduler == null) {
        return;
      }
      stopping = scheduler;
      scheduler = null;
    }
    stopping.shutdownNow();
    try {
      stopping.awaitTermination(SHUTDOWN_WAIT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void tickAll() {
    try {
      for (NetworkMatch match : matches.values()) {
        try {
          tickOne(match);
        } catch (RuntimeException e) {
          System.out.println("match " + match.getId() + " failed a tick: " + e);
          matches.remove(match.getId());
          match.markFinished();
        }
      }
    } catch (RuntimeException e) {
      System.out.println("match clock error: " + e);
    }
  }

  private void tickOne(NetworkMatch match) {
    if (match.isFinished()) {
      retire(match);
      return;
    }
    match.getState().tick();
    Listener current = listener;
    if (current != null) {
      current.onTick(match);
    }
    if (match.isFinished()) {
      retire(match);
    }
  }

  private void retire(NetworkMatch match) {
    matches.remove(match.getId());
    if (!match.claimEnded()) {
      return;
    }
    Listener current = listener;
    if (current != null) {
      current.onFinished(match);
    }
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
