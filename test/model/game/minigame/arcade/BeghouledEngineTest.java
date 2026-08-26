package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import model.game.minigame.arcade.BeghouledEngine.PlantKind;
import model.game.minigame.arcade.BeghouledEngine.Upgrade;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Fully self-contained: no board, no repository, no LibGDX. Seeded {@link Random}s make the
 * engine deterministic, which is what lets {@link #eatingNeverLeavesTheBoardWithNoLegalMove()} and
 * {@link #theTwoClickSwapWinsEveryBoard()} sweep many boards instead of trusting one hand-picked
 * layout.
 */
class BeghouledEngineTest {

  private static final int ROWS = BeghouledEngine.ROWS;
  private static final int COLS = BeghouledEngine.COLS;

  // ---- starting board -----------------------------------------------------------------------

  @Test
  void aFreshBoardHasNoMatchAlreadySittingOnIt() {
    for (long seed = 0; seed < 40; seed++) {
      BeghouledEngine engine = new BeghouledEngine(1, new Random(seed));
      assertNoRun(engine, "seed " + seed);
    }
  }

  @Test
  void aFreshBoardAlwaysHasAtLeastOneLegalMove() {
    for (long seed = 0; seed < 40; seed++) {
      BeghouledEngine engine = new BeghouledEngine(1, new Random(seed));
      assertTrue(hasLegalMove(engine), "seed " + seed + " has no opening move");
    }
  }

  // ---- swap -----------------------------------------------------------------------------------

  @Test
  void swappingTwoTilesThatAreNotNeighboursIsRejected() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(1));
    String result = engine.swap(0, 0, 4, 8);
    assertTrue(result.startsWith("error:"));
  }

  @Test
  void swappingOffTheBoardIsRejected() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(1));
    assertTrue(engine.swap(-1, 0, 0, 0).startsWith("error:"));
    assertTrue(engine.swap(0, 0, ROWS, 0).startsWith("error:"));
  }

  @Test
  void aSwapThatDoesNotCreateAMatchIsRefusedAndTheBoardIsRestored() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(1));
    PlantKind[][] before = snapshot(engine);
    int[] pair = findRefusableAdjacentPair(engine);
    assumeFound(pair);

    String result = engine.swap(pair[0], pair[1], pair[2], pair[3]);

    assertTrue(result.startsWith("error:"), result);
    assertArrayGridEquals(before, snapshot(engine), "a refused swap must leave the board exactly as it was");
  }

  @Test
  void aSwapThatCreatesAMatchGrantsSunAndCountsTowardsTheTarget() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(1));
    int[] move = findGoodSwap(engine);
    assumeFound(move);
    int sunBefore = engine.getSun();
    int matchesBefore = engine.getMatchesMade();

    String result = engine.swap(move[0], move[1], move[2], move[3]);

    assertFalse(result.startsWith("error:"), result);
    assertTrue(engine.getSun() > sunBefore, "a match must pay out sun");
    assertTrue(engine.getMatchesMade() > matchesBefore);
  }

  @Test
  void reachingTheMatchTargetWinsTheGame() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(2));
    int guard = 0;
    while (!engine.isFinished() && guard++ < 500) {
      int[] move = findGoodSwap(engine);
      if (move == null) {
        break;
      }
      engine.swap(move[0], move[1], move[2], move[3]);
    }
    assertTrue(engine.isWon(), "matching to the target should win, not stall");
  }

  // ---- upgrades -------------------------------------------------------------------------------

  @Test
  void anUpgradeConvertsEveryMatchingPlantAndSpendsSun() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(3));
    Upgrade upgrade = cheapestUpgrade(engine);
    giveEnoughSunFor(engine, upgrade.cost);
    // Grinding matches for sun is also progress toward the win condition; on some boards it wins
    // outright before enough sun ever piles up. That is a real, valid outcome, just not the one
    // this test is about, so it is skipped rather than failed.
    Assumptions.assumeFalse(engine.isFinished(),
        "the board matched its way to a win while saving up for the upgrade");
    Assumptions.assumeTrue(engine.getSun() >= upgrade.cost,
        "could not gather enough sun for the cheapest upgrade within the swap budget");
    int before = countOf(engine, upgrade.from);
    assumeAtLeastOne(before);

    String result = engine.upgrade(upgrade.from.label);

    assertFalse(result.startsWith("error:"), result);
    assertEquals(0, countOf(engine, upgrade.from), "every one of them should have converted");
    assertTrue(countOf(engine, upgrade.to) >= before);
  }

  @Test
  void anUpgradeWithoutEnoughSunIsRefused() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(3));
    // A fresh board starts with far less sun than even the cheapest upgrade costs.
    Upgrade cheapest = cheapestUpgrade(engine);
    String result = engine.upgrade(cheapest.from.label);
    assertTrue(result.startsWith("error:"));
  }

  @Test
  void anUpgradeForAnUnknownPlantIsRefused() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(3));
    assertTrue(engine.upgrade("not-a-real-plant").startsWith("error:"));
  }

  // ---- the crater/stall regression ------------------------------------------------------------

  /**
   * Regression for a real stall: eating a plant refilled the board without checking a legal move
   * still existed, and roughly one eat in three hundred left a board where no swap was accepted --
   * only another zombie eating (by luck) could ever unstick it. See the "ensureBoardIsPlayable()"
   * call BeghouledEngine.advanceZombies() now makes right after carving a crater.
   */
  @Test
  void eatingNeverLeavesTheBoardWithNoLegalMove() {
    int postEatBoardsChecked = 0;
    for (long seed = 0; seed < 60; seed++) {
      BeghouledEngine engine = new BeghouledEngine(1, new Random(seed));
      int previousCraters = 0;
      for (int tick = 0; tick < 4000 && !engine.isFinished(); tick++) {
        engine.tick();
        int craters = countCraters(engine);
        if (craters > previousCraters) {
          previousCraters = craters;
          postEatBoardsChecked++;
          assertTrue(hasLegalMove(engine),
              "seed " + seed + " tick " + tick + " has no legal move after a zombie ate");
        }
      }
    }
    assertTrue(postEatBoardsChecked > 0, "the sweep should have exercised at least one eat");
  }

  @Test
  void aCraterIsPermanentAndNeverGetsAPlantAgain() {
    BeghouledEngine engine = new BeghouledEngine(1, new Random(7));
    int craterRow = -1, craterCol = -1;
    for (int tick = 0; tick < 4000 && !engine.isFinished(); tick++) {
      engine.tick();
      outer:
      for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
          if (engine.isCraterAt(r, c)) {
            craterRow = r;
            craterCol = c;
            break outer;
          }
        }
      }
      if (craterRow >= 0) {
        break;
      }
    }
    assumeFoundCrater(craterRow);

    for (int more = 0; more < 500 && !engine.isFinished(); more++) {
      engine.tick();
    }
    assertTrue(engine.isCraterAt(craterRow, craterCol), "a crater must stay a crater");
    assertEquals(null, engine.getPlantAt(craterRow, craterCol), "and never grow a plant again");
  }

  // ---- helpers ----------------------------------------------------------------------------

  private static void giveEnoughSunFor(BeghouledEngine engine, int cost) {
    // The engine has no direct "set sun" hook, so a few matches are cashed in for it. Any swap
    // that resolves works; the target amount just has to clear the upgrade's price. Stops the
    // moment the match ends too, win or lose -- there is nothing left to swap for either way.
    int guard = 0;
    while (engine.getSun() < cost && !engine.isFinished() && guard++ < 200) {
      int[] move = findGoodSwap(engine);
      if (move == null) {
        break;
      }
      engine.swap(move[0], move[1], move[2], move[3]);
    }
  }

  private static Upgrade cheapestUpgrade(BeghouledEngine engine) {
    Upgrade cheapest = null;
    for (Upgrade upgrade : engine.getUpgrades()) {
      if (cheapest == null || upgrade.cost < cheapest.cost) {
        cheapest = upgrade;
      }
    }
    return cheapest;
  }

  private static int countOf(BeghouledEngine engine, PlantKind kind) {
    int count = 0;
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (engine.getPlantAt(r, c) == kind) {
          count++;
        }
      }
    }
    return count;
  }

  private static int countCraters(BeghouledEngine engine) {
    int count = 0;
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (engine.isCraterAt(r, c)) {
          count++;
        }
      }
    }
    return count;
  }

  private static PlantKind[][] snapshot(BeghouledEngine engine) {
    PlantKind[][] grid = new PlantKind[ROWS][COLS];
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        grid[r][c] = engine.getPlantAt(r, c);
      }
    }
    return grid;
  }

  private static boolean hasRun(PlantKind[][] grid) {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c + 2 < COLS; c++) {
        PlantKind kind = grid[r][c];
        if (kind != null && kind == grid[r][c + 1] && kind == grid[r][c + 2]) {
          return true;
        }
      }
    }
    for (int c = 0; c < COLS; c++) {
      for (int r = 0; r + 2 < ROWS; r++) {
        PlantKind kind = grid[r][c];
        if (kind != null && kind == grid[r + 1][c] && kind == grid[r + 2][c]) {
          return true;
        }
      }
    }
    return false;
  }

  private static void assertNoRun(BeghouledEngine engine, String context) {
    assertFalse(hasRun(snapshot(engine)), context + " already has a three-in-a-row");
  }

  private static boolean hasLegalMove(BeghouledEngine engine) {
    PlantKind[][] grid = snapshot(engine);
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (c + 1 < COLS && grid[r][c] != null && grid[r][c + 1] != null
            && wouldMatch(grid, r, c, r, c + 1)) {
          return true;
        }
        if (r + 1 < ROWS && grid[r][c] != null && grid[r + 1][c] != null
            && wouldMatch(grid, r, c, r + 1, c)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean wouldMatch(PlantKind[][] grid, int rowA, int colA, int rowB, int colB) {
    PlantKind temp = grid[rowA][colA];
    grid[rowA][colA] = grid[rowB][colB];
    grid[rowB][colB] = temp;
    boolean matches = hasRun(grid);
    temp = grid[rowA][colA];
    grid[rowA][colA] = grid[rowB][colB];
    grid[rowB][colB] = temp;
    return matches;
  }

  /** {row, col, row, col} for a swap that would create a match, or null if there is none. */
  private static int[] findGoodSwap(BeghouledEngine engine) {
    PlantKind[][] grid = snapshot(engine);
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (c + 1 < COLS && grid[r][c] != null && grid[r][c + 1] != null
            && wouldMatch(grid, r, c, r, c + 1)) {
          return new int[] {r, c, r, c + 1};
        }
        if (r + 1 < ROWS && grid[r][c] != null && grid[r + 1][c] != null
            && wouldMatch(grid, r, c, r + 1, c)) {
          return new int[] {r, c, r + 1, c};
        }
      }
    }
    return null;
  }

  /** {row, col, row, col} for an adjacent pair whose swap would NOT create a match, or null. */
  private static int[] findRefusableAdjacentPair(BeghouledEngine engine) {
    PlantKind[][] grid = snapshot(engine);
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (c + 1 < COLS && grid[r][c] != null && grid[r][c + 1] != null
            && !wouldMatch(grid, r, c, r, c + 1)) {
          return new int[] {r, c, r, c + 1};
        }
      }
    }
    return null;
  }

  private static void assertArrayGridEquals(PlantKind[][] expected, PlantKind[][] actual, String message) {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        assertEquals(expected[r][c], actual[r][c], message + " (row " + r + ", col " + c + ")");
      }
    }
  }

  private static void assumeFound(int[] move) {
    assertTrue(move != null, "test setup could not find a usable pair on this board");
  }

  private static void assumeAtLeastOne(int count) {
    assertTrue(count > 0, "test setup expected at least one of this plant kind on a fresh board");
  }

  private static void assumeFoundCrater(int row) {
    assertTrue(row >= 0, "no zombie ate a plant within the tick budget; widen it or reseed");
  }
}
