package view.gdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The fall-matching behind Beghouled's settle.
 *
 * <p>Worth pinning because it fails quietly: get the direction backwards and the tiles rise out of
 * the board instead of dropping into it, and mishandle a crater and tiles fall through solid
 * ground. None of that throws.
 */
class GridMotionTest {

  private static final int ROWS = 5;
  private static final int COLUMNS = 3;

  private static String[][] grid(String... rowsTopFirst) {
    String[][] cells = new String[ROWS][COLUMNS];
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        char glyph = rowsTopFirst[row].charAt(col);
        cells[row][col] = glyph == '.' ? null : String.valueOf(glyph);
      }
    }
    return cells;
  }

  @Test
  void aGridThatDidNotChangeIsNotSettling() {
    GridMotion motion = new GridMotion(ROWS, COLUMNS);
    String[][] same = grid("abc", "abc", "abc", "abc", "abc");
    motion.observe(same, grid("abc", "abc", "abc", "abc", "abc"));
    assertFalse(motion.isSettling(), "nothing moved, so nothing should be drawn moving");
    assertEquals(0f, motion.liftOf(4, 0), 1e-6);
  }

  /**
   * Column 0 loses its bottom tile, so everything above it comes down one row and a new tile
   * arrives over the top. Gravity in this grid pulls towards the higher row index.
   */
  @Test
  void tilesAboveAClearedOneFallIntoIt() {
    GridMotion motion = new GridMotion(ROWS, COLUMNS);
    String[][] before = grid("pbc", "qbc", "rbc", "sbc", "zbc");
    String[][] after = grid("nbc", "pbc", "qbc", "rbc", "sbc");
    motion.observe(before, after);

    assertTrue(motion.isSettling(), "a whole column moved and nothing was drawn moving");
    for (int row = 1; row <= 4; row++) {
      assertEquals(1f, fallOf(motion, row, 0), 1e-4,
          "the tile now at row " + row + " came from one row above it");
    }
    assertEquals(1f, fallOf(motion, 0, 0), 1e-4,
        "the refilled tile at the top came in from just over the edge");
    assertEquals(0f, fallOf(motion, 4, 1), 1e-4, "the untouched column must not move");
  }

  /** Three cleared in one column: the survivor above them falls the full three rows. */
  @Test
  void aTileFallsAsFarAsTheGapBeneathIt() {
    GridMotion motion = new GridMotion(ROWS, COLUMNS);
    String[][] before = grid("sbc", "xbc", "xbc", "xbc", "tbc");
    String[][] after = grid("nbc", "mbc", "kbc", "sbc", "tbc");
    motion.observe(before, after);

    assertEquals(0f, fallOf(motion, 4, 0), 1e-4, "the bottom tile never moved");
    assertEquals(3f, fallOf(motion, 3, 0), 1e-4, "the survivor should drop the three cleared rows");
  }

  /**
   * A crater splits its column. Nothing falls through one, so the tile under it must be matched
   * against the tile that was under it and never against something from above.
   */
  @Test
  void nothingFallsThroughACrater() {
    GridMotion motion = new GridMotion(ROWS, COLUMNS);
    String[][] before = grid("pbc", "qbc", ".bc", "sbc", "tbc");
    String[][] after = grid("nbc", "pbc", ".bc", "sbc", "tbc");
    motion.observe(before, after);

    assertEquals(0f, fallOf(motion, 2, 0), 1e-4, "a crater does not move");
    assertEquals(0f, fallOf(motion, 3, 0), 1e-4,
        "the tile below the crater was untouched and must not be dragged down through it");
    assertEquals(0f, fallOf(motion, 4, 0), 1e-4, "nor the one below that");
    assertEquals(1f, fallOf(motion, 1, 0), 1e-4, "above the crater the column still compacts");
  }

  @Test
  void theSettleRunsOutAndLeavesEverythingWhereItBelongs() {
    GridMotion motion = new GridMotion(ROWS, COLUMNS);
    motion.observe(grid("pbc", "qbc", "rbc", "sbc", "zbc"),
        grid("nbc", "pbc", "qbc", "rbc", "sbc"));
    assertTrue(motion.liftOf(2, 0) > 0f, "it should start clear of its square");
    motion.advance(GridMotion.SETTLE_SECONDS);
    assertFalse(motion.isSettling(), "the settle should be over");
    assertEquals(0f, motion.liftOf(2, 0), 1e-6, "and every tile sitting exactly on its square");
  }

  /** liftOf is eased, so the fall distance is read back at the very start of the settle. */
  private static float fallOf(GridMotion motion, int row, int column) {
    return motion.liftOf(row, column);
  }
}
