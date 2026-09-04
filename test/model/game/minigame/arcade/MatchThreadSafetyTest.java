package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import model.enums.MatchRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * A match is ticked by the server's clock thread and read by each player's connection thread.
 *
 * <p>{@code MatchService} runs every live match on one scheduled thread, while a player's move
 * arrives on that player's own connection thread and is answered by broadcasting a fresh snapshot
 * from that same thread. Both walk the engine's lists of plants, zombies and shots, and the shots
 * list changes on nearly every tick -- so a snapshot taken mid-tick used to be able to come apart
 * in the middle of building itself and answer the player with a server error instead of the board.
 */
class MatchThreadSafetyTest {

  /** A match is over after two minutes of ticks, so the racing is spread across a run of them. */
  private static final int MATCHES = 40;

  @Test
  @Timeout(120)
  void snapshotsTakenWhileTheClockIsRunningDoNotComeApart() throws InterruptedException {
    AtomicReference<IZombieMatch> live = new AtomicReference<>(new IZombieMatch(1, 11L));
    AtomicReference<Throwable> broke = new AtomicReference<>();
    AtomicBoolean running = new AtomicBoolean(true);

    // The clock, as MatchService drives it, plus a player feeding it zombies so the cutouts have
    // something to shoot at: an empty lane keeps no shots in flight and races with nothing.
    Thread clock = new Thread(() -> {
      try {
        for (int round = 0; round < MATCHES && broke.get() == null; round++) {
          IZombieMatch match = new IZombieMatch(1, 11L + round);
          live.set(match);
          for (int tick = 0; !match.isFinished(); tick++) {
            match.tick();
            match.apply(MatchRole.ZOMBIES, IZombieAction.placeZombie("basic", tick % 5, 7));
          }
        }
      } catch (Throwable failure) {
        broke.compareAndSet(null, failure);
      } finally {
        running.set(false);
      }
    }, "match-clock");

    List<Thread> readers = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      readers.add(new Thread(() -> {
        try {
          while (running.get()) {
            IZombieMatch.Snapshot state = live.get().snapshot();
            assertNotNull(state.plants());
            assertNotNull(state.zombies());
            assertNotNull(state.shots());
          }
        } catch (Throwable failure) {
          broke.compareAndSet(null, failure);
        }
      }, "client-connection-" + i));
    }

    clock.start();
    readers.forEach(Thread::start);
    clock.join();
    for (Thread reader : readers) {
      reader.join();
    }

    assertTrue(broke.get() == null,
        "reading a match while it was being ticked threw " + broke.get());
  }
}
