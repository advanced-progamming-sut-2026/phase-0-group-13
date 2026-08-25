package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.enums.MatchRole;
import org.junit.jupiter.api.Test;

/** The two ways a networked I, Zombie match can end, and what it does once it has. */
class IZombieMatchTest {

  private static final long SEED = 20260825L;

  @Test
  void theDefenderWinsBySurvivingTheFullTwoMinutes() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    tick(match, IZombieMatch.SURVIVAL_TICKS);

    assertTrue(match.isFinished(), "two minutes should have run out");
    assertEquals(MatchRole.PLANTS, match.winner());
    assertEquals(0, match.ticksRemaining());
  }

  @Test
  void theClockRunsOutOnExactlyTheDefinedTick() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    tick(match, IZombieMatch.SURVIVAL_TICKS - 1);

    assertFalse(match.isFinished(), "one tick short of two minutes is still a live match");
    assertNull(match.winner());
    assertEquals(1, match.ticksRemaining());

    match.tick();

    assertTrue(match.isFinished());
    assertEquals(MatchRole.PLANTS, match.winner());
    assertEquals(IZombieMatch.SURVIVAL_TICKS, match.getTick());
  }

  @Test
  void aFinishedMatchDoesNotKeepTicking() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    tick(match, IZombieMatch.SURVIVAL_TICKS);
    int endedAt = match.getTick();

    tick(match, 200);

    assertEquals(endedAt, match.getTick(), "the clock stopped when the match did");
    assertEquals(MatchRole.PLANTS, match.winner(), "the verdict never changes");
  }

  @Test
  void aFinishedMatchRefusesFurtherActions() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    tick(match, IZombieMatch.SURVIVAL_TICKS);

    assertEquals("error: the match is over",
        match.apply(MatchRole.ZOMBIES, IZombieAction.placeZombie("basic", 0, 8)));
    assertEquals("error: the match is over",
        match.apply(MatchRole.PLANTS, IZombieAction.placePlant("Sunflower", 0, 0)));
  }

  @Test
  void theAttackerWinsBeforeTwoMinutesByEatingEveryBrain() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    int lane = 0;

    for (int step = 0; step < IZombieMatch.SURVIVAL_TICKS && !match.isFinished(); step++) {
      IZombieMatch.Snapshot board = match.snapshot();
      while (lane < IZombieEngine.BRAINS && !board.brains()[lane]) {
        lane++;
      }
      if (lane < IZombieEngine.BRAINS) {
        push(match, board, lane);
      }
      match.tick();
    }

    assertTrue(match.isFinished(), "a focused attack should get through inside two minutes");
    assertEquals(MatchRole.ZOMBIES, match.winner());
    assertTrue(match.getTick() < IZombieMatch.SURVIVAL_TICKS,
        "won on tick " + match.getTick() + " of " + IZombieMatch.SURVIVAL_TICKS);
    assertEquals(0, match.snapshot().brainsRemaining());
  }

  /** Sends the toughest zombie the purse can currently afford into one lane. */
  private static void push(IZombieMatch match, IZombieMatch.Snapshot board, int lane) {
    IZombieEngine.ZombieSpec best = null;
    for (IZombieEngine.ZombieSpec spec : IZombieEngine.zombieTypesFor(1)) {
      Integer waiting = board.zombieRecharge().get(spec.name);
      boolean ready = waiting == null || waiting == 0;
      if (ready && spec.cost <= board.zombieSun() && (best == null || spec.health > best.health)) {
        best = spec;
      }
    }
    if (best != null) {
      match.apply(MatchRole.ZOMBIES,
          IZombieAction.placeZombie(best.name, lane, IZombieEngine.COLS - 1));
    }
  }

  @Test
  void aRoleCannotMakeTheOtherSidesMove() {
    IZombieMatch match = new IZombieMatch(1, SEED);

    assertEquals("error: only the zombie player can place zombies",
        match.apply(MatchRole.PLANTS, IZombieAction.placeZombie("basic", 0, 8)));
    assertEquals("error: only the plant player can place plants",
        match.apply(MatchRole.ZOMBIES, IZombieAction.placePlant("Sunflower", 0, 0)));
  }

  @Test
  void anAppliedMoveIsReportedAsAccepted() {
    IZombieMatch match = new IZombieMatch(1, SEED);

    assertNull(match.apply(MatchRole.ZOMBIES, IZombieAction.placeZombie("basic", 2, 8)));
    assertNull(match.apply(MatchRole.PLANTS, IZombieAction.placePlant("Sunflower", 2, 0)));
  }

  @Test
  void theSnapshotCarriesTheWholeBoard() {
    IZombieMatch match = new IZombieMatch(1, SEED);
    match.apply(MatchRole.ZOMBIES, IZombieAction.placeZombie("basic", 3, 8));
    match.tick();

    IZombieMatch.Snapshot board = match.snapshot();
    assertEquals(1, board.tick());
    assertEquals(IZombieMatch.SURVIVAL_TICKS - 1, board.ticksRemaining());
    assertEquals(IZombieEngine.BRAINS, board.brains().length);
    assertFalse(board.plants().isEmpty(), "the seeded cutouts are on the board");
    assertNotNull(board.plants().get(0).name());
    assertFalse(board.zombies().isEmpty());
    assertFalse(board.zombieRecharge().isEmpty());
    assertFalse(board.plantRecharge().isEmpty());
  }

  private static void tick(IZombieMatch match, int times) {
    for (int i = 0; i < times; i++) {
      match.tick();
    }
  }
}
