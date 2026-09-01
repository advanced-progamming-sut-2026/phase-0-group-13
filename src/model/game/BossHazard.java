package model.game;

import java.util.Random;
import model.enums.StatusEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * Something a Zomboss sends at the lawn that takes time to arrive.
 *
 * <p>The bosses used to destroy a plant on the same tick they decided to: a missile was a printf
 * and a dead plant, with nothing on screen in between, no warning, and no way for the player to
 * read what had happened. Each of these instead lives on the board for a while -- a missile and an
 * ice boulder falling onto the tile they are aimed at, a shark swimming up a row -- and does its
 * damage only when it gets there, which gives the renderer something to draw and the player a
 * moment to see it coming.
 *
 * <p>The hazard carries its own landing behaviour rather than leaving it to the board, so the
 * board can advance a mixed list without knowing what any of them are.
 */
public class BossHazard {

  /** How high above its row a falling hazard starts, in lanes, for the renderer to draw it. */
  public static final double FALL_HEIGHT_LANES = 4.5;

  private static final int MISSILE_GRAVES = 2;
  private static final int GRAVE_HEALTH = 500;
  private static final int FREEZE_TICKS = 200;
  private static final int DESTROY_DAMAGE = 100_000;
  private static final double ICE_SPLASH_REACH = 1.0;

  private static final Random RANDOM = new Random();

  public enum Kind {
    /** Egypt's rocket: flattens the tile it lands on and throws up graves around it. */
    MISSILE,
    /** The mammoth's slingshot: flattens the tile and leaves it frozen over. */
    ICE_BOULDER,
    /** A beach shark, swimming up its row and eating the first plant it reaches. */
    SHARK
  }

  private final Kind kind;
  private final int row;
  private final double speed;
  private final int flightTicks;

  private double column;
  private double previousColumn;
  private int ticksLeft;
  private boolean active = true;

  private BossHazard(Kind kind, int row, double column, int flightTicks, double speed) {
    this.kind = kind;
    this.row = row;
    this.column = column;
    this.previousColumn = column;
    this.flightTicks = Math.max(1, flightTicks);
    this.ticksLeft = this.flightTicks;
    this.speed = speed;
  }

  /** Egypt's rocket, arcing down onto one tile. */
  public static BossHazard missile(int row, int column, int flightTicks) {
    return new BossHazard(Kind.MISSILE, row, column, flightTicks, 0);
  }

  /** The mammoth's boulder, the same flight with a colder landing. */
  public static BossHazard iceBoulder(int row, int column, int flightTicks) {
    return new BossHazard(Kind.ICE_BOULDER, row, column, flightTicks, 0);
  }

  /** A shark let loose at the far end of a row, swimming towards the house. */
  public static BossHazard shark(int row, double fromColumn, double speed) {
    return new BossHazard(Kind.SHARK, row, fromColumn, Integer.MAX_VALUE, speed);
  }

  public Kind getKind() {
    return kind;
  }

  public int getRow() {
    return row;
  }

  public double getColumn() {
    return column;
  }

  /** Where it was last tick, so the view can draw it between the two. */
  public double getPreviousColumn() {
    return previousColumn;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isFalling() {
    return kind != Kind.SHARK;
  }

  /**
   * How far a falling hazard still has to drop, 1 at launch down to 0 on impact.
   *
   * <p>Squared, so it accelerates downwards instead of drifting at a constant rate.
   */
  public double fallFraction() {
    if (!isFalling()) {
      return 0;
    }
    double left = ticksLeft / (double) flightTicks;
    return left * left;
  }

  public void advance() {
    previousColumn = column;
    if (isFalling()) {
      if (ticksLeft > 0) {
        ticksLeft--;
      }
      return;
    }
    column -= speed;
  }

  /** True once a falling hazard has reached the ground. Never true of a shark. */
  public boolean hasLanded() {
    return isFalling() && ticksLeft <= 0;
  }

  /** True once a shark has swum off the near end of the lawn without finding anything. */
  public boolean hasLeftTheLawn() {
    return !isFalling() && column < -0.5;
  }

  public void spend() {
    active = false;
  }

  /**
   * Does whatever this hazard came to do.
   *
   * @param board the lawn it landed on
   */
  public void land(Board board) {
    if (!active) {
      return;
    }
    active = false;
    int col = (int) Math.round(column);
    if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getColumns()) {
      return;
    }
    switch (kind) {
      case MISSILE -> landMissile(board, col);
      case ICE_BOULDER -> landBoulder(board, col);
      case SHARK -> eatWhatItReached(board, col);
      default -> { }
    }
  }

  private void landMissile(Board board, int col) {
    flatten(board, row, col);
    System.out.printf("The missile landed on (%d, %d)!%n", col + 1, row + 1);
    for (int i = 0; i < MISSILE_GRAVES; i++) {
      int graveRow = RANDOM.nextInt(board.getRows());
      int graveCol = RANDOM.nextInt(board.getColumns());
      if (board.getPlantAt(graveRow, graveCol) != null) {
        continue;
      }
      board.placeTileEffect(graveRow, graveCol, new TombStoneEffect(GRAVE_HEALTH, true));
      System.out.printf("The blast threw up a grave at (%d, %d).%n", graveCol + 1, graveRow + 1);
    }
  }

  private void landBoulder(Board board, int col) {
    flatten(board, row, col);
    board.placeTileEffect(row, col, new IceTrailEffect(FREEZE_TICKS, 0.0, true));
    System.out.printf("The boulder shattered over (%d, %d) and froze it solid.%n",
            col + 1, row + 1);
    // Its own zombies are caught in it too, which is what makes the boulder worth dodging round
    // rather than something the player simply watches land.
    for (Zombie other : board.getZombies()) {
      if (!other.isDead() && !other.isBoss() && other.occupiesRow(row)
              && Math.abs(other.getX() - col) <= ICE_SPLASH_REACH) {
        other.applyEffect(StatusEffect.FROZEN, FREEZE_TICKS);
      }
    }
  }

  private void eatWhatItReached(Board board, int col) {
    Plant prey = board.getPlantAt(row, col);
    if (prey != null && !prey.isDead()) {
      System.out.printf("A shark ate the %s at (%d, %d)!%n", prey.getName(), col + 1, row + 1);
      prey.takeDamage(DESTROY_DAMAGE);
    }
  }

  private static void flatten(Board board, int row, int col) {
    Plant plant = board.getPlantAt(row, col);
    if (plant != null && !plant.isDead()) {
      plant.takeDamage(DESTROY_DAMAGE);
    }
  }
}
