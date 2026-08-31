package view.gdx.render;


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

  public float columnToX(double column) {
    return originX + (float) column * cellWidth;
  }

  /** Bottom edge of a lane, flipped because libGDX y goes up. */
  public float rowToY(int row) {
    return originY + (rows - 1 - row) * cellHeight;
  }

  public float columnCentreX(double column) {
    return columnToX(column) + cellWidth / 2f;
  }

  public float rowCentreY(int row) {
    return rowToY(row) + cellHeight / 2f;
  }

  public int xToColumn(float worldX) {
    if (cellWidth <= 0f) {
      return -1;
    }
    int column = (int) Math.floor((worldX - originX) / cellWidth);
    return column >= 0 && column < columns ? column : -1;
  }

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
