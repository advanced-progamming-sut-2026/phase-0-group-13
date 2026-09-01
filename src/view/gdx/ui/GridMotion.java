package view.gdx.ui;

/**
 * How far each tile of a match-three grid has just fallen, so a board can be drawn settling.
 *
 * <p>Beghouled's engine resolves a swap in one call: the matches clear, the columns compact and
 * the gaps refill before {@code swap} has returned, so the screen only ever saw the finished grid
 * and the board changed between one frame and the next with nothing in between. Rather than pull
 * the engine apart, this works out what moved by comparing two pictures of the grid and hands the
 * screen an offset per cell, which is enough to draw the fall.
 *
 * <p>Kept clear of the engine and of libGDX on purpose: it takes two arrays of labels and returns
 * numbers, so the matching can be tested without a board or a GL context.
 */
public final class GridMotion {

  /** How long a settle takes. Long enough to read as a fall, short enough not to be waited on. */
  public static final float SETTLE_SECONDS = 0.26f;

  private final int rows;
  private final int columns;
  private final float[][] fell;
  private float settleLeft;

  public GridMotion(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
    this.fell = new float[rows][columns];
  }

  /**
   * Works out what fell where between two pictures of the same grid.
   *
   * <p>Columns are walked from the bottom up with a reading finger in the older picture: a tile
   * that is still there matches the lowest surviving tile of its kind that has not been claimed
   * yet, and the gap between the two is how far it fell. Anything left over when the finger runs
   * off the top of the column is new, and came in over the top.
   *
   * <p>Matching by kind alone can pair up two identical tiles the wrong way round. It does not
   * matter: one Sunflower falling into another Sunflower's place is the same picture either way.
   *
   * @param before the grid as it was, one label per cell and null for a crater or a gap
   * @param after the grid as it is now, the same shape
   */
  public void observe(String[][] before, String[][] after) {
    if (before == null || after == null) {
      return;
    }
    boolean anythingMoved = false;
    for (int column = 0; column < columns; column++) {
      int reading = rows - 1;
      for (int row = rows - 1; row >= 0; row--) {
        fell[row][column] = 0f;
        String wanted = after[row][column];
        if (wanted == null) {
          // A crater, and craters split a column: nothing falls past one, so the finger starts
          // again above it rather than matching a tile through solid ground.
          reading = row - 1;
          continue;
        }
        while (reading >= 0 && !wanted.equals(before[reading][column])) {
          reading--;
        }
        if (reading >= 0) {
          fell[row][column] = row - reading;
          reading--;
        } else {
          // Nothing left below it in the old picture, so this one was refilled from off the top.
          fell[row][column] = row + 1f;
        }
        anythingMoved |= fell[row][column] > 0f;
      }
    }
    if (anythingMoved) {
      settleLeft = SETTLE_SECONDS;
    }
  }

  public void advance(float delta) {
    if (settleLeft > 0f) {
      settleLeft = Math.max(0f, settleLeft - delta);
    }
  }

  public boolean isSettling() {
    return settleLeft > 0f;
  }

  /**
   * How far above its resting place this cell should be drawn right now, in rows.
   *
   * <p>Zero once the settle is over, which is every frame in which nothing has just happened.
   * Eased so the tiles come down quickly and land gently instead of sliding at a constant rate.
   */
  public float liftOf(int row, int column) {
    if (settleLeft <= 0f || row < 0 || row >= rows || column < 0 || column >= columns) {
      return 0f;
    }
    float left = settleLeft / SETTLE_SECONDS;
    return fell[row][column] * left * left;
  }
}
