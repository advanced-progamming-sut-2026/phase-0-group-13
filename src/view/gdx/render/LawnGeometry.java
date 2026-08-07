package view.gdx.render;


/**
 * Converts lawn coordinates into world pixels.
 *
 * <p>The only place that knows the mapping, so the renderers and the input handler can't end up
 * disagreeing about where a tile is. Two things it deals with:
 *
 * <ul>
 *   <li>the model uses a double for the column (Zombie.getX(), Sun.getX(), Projectile) and not an
 *       int, so entities can sit between tiles without changing anything in the model;
 *   <li>row 0 is the top lane but libGDX y goes up, so rowToY() flips it.
 * </ul>
 *
 * <p>Takes the size from the board instead of a constant, so minigame boards work too.
 */
public final class LawnGeometry {

  private final int rows;
  private final int columns;

  private float originX;
  private float originY;
  private float cellWidth;
  private float cellHeight;

  public LawnGeometry(int rows, int columns) {
    this.rows = Math.max(rows, 1);
    this.columns = Math.max(columns, 1);
  }

  /**
   * Puts the lawn somewhere in the world and works out the cell size.
   *
   * @param x left edge
   * @param y bottom edge
   */
  public void setBounds(float x, float y, float width, float height) {
    this.originX = x;
    this.originY = y;
    this.cellWidth = width / columns;
    this.cellHeight = height / rows;
  }

  /** Left edge of a column. Takes fractions, which is how entities move. */
  public float columnToX(double column) {
    return originX + (float) column * cellWidth;
  }

  /** Bottom edge of a lane, flipped because libGDX y goes up. */
  public float rowToY(int row) {
    return originY + (rows - 1 - row) * cellHeight;
  }

  /** Middle of a tile on x. */
  public float columnCentreX(double column) {
    return columnToX(column) + cellWidth / 2f;
  }

  /** Middle of a lane on y. */
  public float rowCentreY(int row) {
    return rowToY(row) + cellHeight / 2f;
  }

  /** Column at a world x, or -1 if it's outside the lawn. */
  public int xToColumn(float worldX) {
    if (cellWidth <= 0f) {
      return -1;
    }
    int column = (int) Math.floor((worldX - originX) / cellWidth);
    return column >= 0 && column < columns ? column : -1;
  }

  /** Row at a world y, or -1 if it's outside. Undoes the flip from rowToY. */
  public int yToRow(float worldY) {
    if (cellHeight <= 0f) {
      return -1;
    }
    int flipped = (int) Math.floor((worldY - originY) / cellHeight);
    int row = rows - 1 - flipped;
    return row >= 0 && row < rows ? row : -1;
  }

  public int getRows() {
    return rows;
  }

  public int getColumns() {
    return columns;
  }

  public float getCellWidth() {
    return cellWidth;
  }

  public float getCellHeight() {
    return cellHeight;
  }
}
