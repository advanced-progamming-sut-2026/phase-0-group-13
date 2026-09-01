package model.game;

/**
 * A dose of plant food lying on the lawn.
 *
 * <p>Plant food is not credited the moment something drops it: it lands where the zombie died or
 * where the grave was, and the player has to go and pick it up before it fades, the same bargain
 * the sun on the lawn asks for. What picking it up does is unchanged -- it still goes through
 * {@link GameState#addPlantFood()} and is still capped there.
 */
public class PlantFoodDrop {

  /** How long a dose sits on the lawn before it fades, in ticks. */
  public static final int LIFETIME_TICKS = 250;

  private final double column;
  private final int row;
  private int ticksLeft = LIFETIME_TICKS;
  private boolean collected;

  public PlantFoodDrop(double column, int row) {
    this.column = column;
    this.row = row;
  }

  public void tick() {
    if (ticksLeft > 0) {
      ticksLeft--;
    }
  }

  /** Whether a click on this tile should pick this dose up. */
  public boolean occupiesTile(int col, int targetRow) {
    return !collected && row == targetRow && Math.abs(column - col) <= 0.5;
  }

  public void markCollected() {
    collected = true;
  }

  public boolean isGone() {
    return collected || ticksLeft <= 0;
  }

  public double getColumn() {
    return column;
  }

  public int getRow() {
    return row;
  }

  public int getTicksLeft() {
    return ticksLeft;
  }

  public boolean isCollected() {
    return collected;
  }
}
