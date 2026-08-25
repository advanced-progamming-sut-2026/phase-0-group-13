package network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import model.enums.MatchRole;
import org.junit.jupiter.api.Test;

/** The server's clock: it runs, it isolates, it retires, and it stops. */
class MatchServiceTest {

  private static final long PATIENCE_MS = 4000;

  @Test
  void anOpenMatchIsAdvancedByTheServer() throws Exception {
    MatchService matches = new MatchService();
    CountDownLatch ticked = new CountDownLatch(3);
    matches.setListener(listener(match -> ticked.countDown(), match -> { }));
    matches.start();
    try {
      NetworkMatch match = matches.create("alice", "bob");
      assertTrue(ticked.await(PATIENCE_MS, TimeUnit.MILLISECONDS),
          "the clock should have stepped the match on its own");
      assertTrue(match.getState().getTick() >= 3);
    } finally {
      matches.shutdown();
    }
  }

  @Test
  void startingTwiceDoesNotGiveAMatchTwoClocks() throws Exception {
    MatchService matches = new MatchService();
    matches.setListener(listener(match -> { }, match -> { }));
    matches.start();
    matches.start();
    try {
      NetworkMatch match = matches.create("alice", "bob");
      Thread.sleep(500);
      int ticks = match.getState().getTick();
      // 500ms at 10 ticks a second is about 5. Two loops would be about 10.
      assertTrue(ticks > 0 && ticks < 9, "ticked " + ticks + " times in half a second");
    } finally {
      matches.shutdown();
    }
  }

  @Test
  void oneBadMatchDoesNotStopTheOthers() throws Exception {
    MatchService matches = new MatchService();
    AtomicInteger goodTicks = new AtomicInteger();
    NetworkMatch[] poisoned = new NetworkMatch[1];
    matches.setListener(listener(match -> {
      if (match == poisoned[0]) {
        throw new IllegalStateException("this match is broken");
      }
      goodTicks.incrementAndGet();
    }, match -> { }));
    matches.start();
    try {
      poisoned[0] = matches.create("alice", "bob");
      matches.create("carol", "dave");
      Thread.sleep(600);
      assertTrue(goodTicks.get() >= 3,
          "the healthy match kept running, ticked " + goodTicks.get() + " times");
      assertNull(matches.get(poisoned[0].getId()), "the broken one was dropped");
    } finally {
      matches.shutdown();
    }
  }

  @Test
  void aFinishedMatchIsReportedOnceAndThenForgotten() throws Exception {
    MatchService matches = new MatchService();
    CountDownLatch finished = new CountDownLatch(1);
    ConcurrentHashMap<String, Integer> endings = new ConcurrentHashMap<>();
    matches.setListener(listener(match -> { }, match -> {
      endings.merge(match.getId(), 1, Integer::sum);
      finished.countDown();
    }));
    NetworkMatch match = matches.create("alice", "bob");
    // Run the board out here rather than waiting two real minutes for the clock to do it.
    while (!match.isFinished()) {
      match.getState().tick();
    }
    matches.start();
    try {
      assertTrue(finished.await(PATIENCE_MS, TimeUnit.MILLISECONDS),
          "the clock should have noticed the match was over");
      Thread.sleep(300);
      assertEquals(1, endings.get(match.getId()), "announced exactly once");
      assertNull(matches.get(match.getId()));
      assertEquals(0, matches.activeCount());
      assertEquals(MatchRole.PLANTS, match.getState().winner());
    } finally {
      matches.shutdown();
    }
  }

  @Test
  void shutdownStopsTheClock() throws Exception {
    MatchService matches = new MatchService();
    matches.setListener(listener(match -> { }, match -> { }));
    matches.start();
    NetworkMatch match = matches.create("alice", "bob");
    Thread.sleep(300);
    matches.shutdown();
    int stoppedAt = match.getState().getTick();

    Thread.sleep(400);

    assertEquals(stoppedAt, match.getState().getTick());
  }

  @Test
  void aMatchKnowsWhoIsPlayingWhichSide() {
    MatchService matches = new MatchService();
    NetworkMatch match = matches.create("alice", "bob");

    assertEquals(MatchRole.PLANTS, match.roleOf("ALICE"));
    assertEquals(MatchRole.ZOMBIES, match.roleOf("bob"));
    assertNull(match.roleOf("mallory"));
    assertEquals("bob", match.opponentOf("alice"));
    assertNotNull(matches.matchOf("bob"));
    assertTrue(match.claimEnded());
    assertTrue(!match.claimEnded(), "only the first caller announces the end");
  }

  private static MatchService.Listener listener(
      java.util.function.Consumer<NetworkMatch> onTick,
      java.util.function.Consumer<NetworkMatch> onFinished) {
    return new MatchService.Listener() {
      @Override
      public void onTick(NetworkMatch match) {
        onTick.accept(match);
      }

      @Override
      public void onFinished(NetworkMatch match) {
        onFinished.accept(match);
      }
    };
  }
}
