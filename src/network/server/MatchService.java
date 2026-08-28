package network.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieMatch;

/**
 * Every live match, and the one clock that advances all of them.
 *
 * <p>The match is the server's, not a client's: nothing a client sends moves a zombie or a plant
 * along, it only asks for a placement. Time itself comes from here, a single daemon thread
 * stepping every open match at {@link IZombieMatch#TICK_MILLIS}, which is the rate the engine's
 * recharges, walking speeds and firing cadences are all written in. One loop for all matches
 * rather than one thread each, so two players cannot end up on two slightly different clocks.
 *
 * <p>A match that throws is dropped rather than allowed to take the loop down with it, and a
 * finished one is handed to the listener exactly once and then forgotten.
 */
public final class MatchService {

  private static final int DEFAULT_LEVEL = 1;

  /** Long enough for any tick to finish, short enough that shutdown can never hang the server. */
  private static final long SHUTDOWN_WAIT_MILLIS = 2000;

  /** What the router wants to know: a board moved, or a match is over. */
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

  /** Idempotent: calling it twice does not give the same match two clocks. */
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

  /**
   * Stops the clock, and does not return until the tick that was in flight has finished.
   *
   * <p>shutdownNow() only interrupts, and a tick is not interruptible -- it is plain arithmetic
   * over the board -- so without the wait a tick that had already started could still land after
   * this returned, and a caller that then read the tick number got a match that was still moving.
   *
   * <p>The scheduler is taken out of the field before the wait so a concurrent start() is not
   * blocked behind it, and the wait is bounded: a listener that calls shutdown from inside its
   * own onTick would otherwise be waiting for itself.
   */
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

  /**
   * Nothing may escape this method: scheduleAtFixedRate stops rescheduling a task that throws, so
   * one bad match would silently freeze every other one.
   */
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

  /** Off the board and reported, once. A second caller only does the removal. */
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

  /** Abandons a match without a winner, for the player who walked out of it. */
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
