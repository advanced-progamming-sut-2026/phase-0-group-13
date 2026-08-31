package model.game;

import model.enums.SunType;

public class Sun {
  public static final int INFINITE_LIFETIME = -1;

  /** How far above its landing tile a sky sun starts, in lanes. It has to clear the top row. */
  private static final double FALL_HEIGHT_ROWS = 7.0;

  private int amount;
  private double x;
  private double y;
  private int timeToLive;
  private SunType sunType;
  private boolean isCollected;
  private int fallingTicks;
  private int fallTotalTicks;
  private double landingY;
  private double previousY;
  private boolean fallPrepared;
  private int groundedTick = -1;

  public Sun(int amount, int timeToLive, SunType sunType) {
    this(amount, timeToLive, sunType, false);
  }

  public Sun(int amount, int timeToLive, SunType sunType, boolean isFalling) {
    this.amount = amount;
    this.timeToLive = timeToLive;
    this.sunType = sunType;
    this.x = 0.0;
    this.y = 0.0;
    this.isCollected = false;
    this.fallingTicks = isFalling ? 50 : 0;
  }

  public void changinCordinate(double x, double y) {
    this.x += x;
    this.y += y;
  }

  /**
   * A sky sun is created on the tile it will land on, so the first tick lifts it back up to the
   * clouds; from there update() walks it down. The landing tile is remembered because that
   * is what collection matches on, and it must not drift while the sun is in the air.
   */
  private void prepareFall() {
    if (fallPrepared || fallingTicks <= 0) {
      return;
    }
    fallPrepared = true;
    fallTotalTicks = fallingTicks;
    landingY = y;
    y = landingY - FALL_HEIGHT_ROWS;
  }

  public void update(int currentTick) {
    prepareFall();
    previousY = y;
    if (fallingTicks > 0) {
      fallingTicks--;
      y = landingY - FALL_HEIGHT_ROWS * (fallingTicks / (double) fallTotalTicks);
      if (fallingTicks == 0) {
        groundedTick = currentTick;
        System.out.printf(
                "Sun reached the ground at position (%d, %d)%n", (int) x + 1, (int) y + 1);
        if (sunType == SunType.RADIOACTIVE) {
          sunType = SunType.NORMAL;
          amount = 25;
        }
      }
    }

    if (timeToLive > 0 && !isCollected) {
      timeToLive--;
    }
  }

  public void collect(GameState state) {
    if (!isCollected && !isExpired()) {
      this.isCollected = true;
      state.addSun(this.amount);
      this.timeToLive = 0;
    }
  }

  public boolean isExpired() {
    if (isCollected) {
      return true;
    }
    return timeToLive != INFINITE_LIFETIME && timeToLive <= 0;
  }

  public int getAmount() { return amount; }
  public void setAmount(int amount) { this.amount = amount; }
  public double getX() { return x; }
  public double getY() { return y; }
  /** The tile this sun belongs to; the same as {@link #getY()} once it has landed. */
  public double getLandingY() { return fallPrepared ? landingY : y; }
  /** Where it hung a tick ago, so the drop is drawn smoothly between ticks. */
  public double getPreviousY() { return previousY; }

  /**
   * Whether a click on this tile should pick this sun up. A sun in mid-air counts both on the tile
   * it is passing over, which is where the player sees it, and on the tile it is heading for, which
   * is where it has always been collectable.
   */
  public boolean occupiesTile(int col, int row) {
    return Math.abs(x - col) <= 0.5
            && (getLandingY() == row || (int) Math.round(y) == row);
  }
  public SunType getType() { return sunType; }
  public void setType(SunType sunType) { this.sunType = sunType; }
  public boolean isFalling() { return fallingTicks > 0; }
  public void setCollected(boolean collected) { this.isCollected = collected; }
  public void setGroundedTick(int tick) { if (this.groundedTick < 0) this.groundedTick = tick; }
  public int getGroundedTick() { return groundedTick; }
}
