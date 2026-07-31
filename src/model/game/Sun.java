package model.game;

import model.enums.SunType;

public class Sun {
  public static final int INFINITE_LIFETIME = -1;

  private int amount;
  private double x;
  private double y;
  private int timeToLive; // به واحد تیک
  private SunType sunType;
  private boolean isCollected;
  private int fallingTicks;

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

  public void update(int currentTick) {
    if (fallingTicks > 0) {
      fallingTicks--;
      if (fallingTicks == 0) {
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
      this.timeToLive = 0; // تیک بعدی پاک می‌شود
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
  public SunType getType() { return sunType; }
  public void setType(SunType sunType) { this.sunType = sunType; }
  public boolean isFalling() { return fallingTicks > 0; }
  public void setCollected(boolean collected) { this.isCollected = collected; }
}