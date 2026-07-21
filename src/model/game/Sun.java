package model.game;

import model.enums.SunType;

public class Sun {
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
      if (fallingTicks == 0 && sunType == SunType.RADIOACTIVE) {
        sunType = SunType.NORMAL;
        amount = 25;
        System.out.println("Radioactive sun reached the ground and turned into a normal sun.");
      }
    }

    if (timeToLive > 0 && !isCollected) {
      timeToLive--;
    }
  }

  public void collect(GameState state) {
    if (!isCollected && timeToLive > 0) {
      this.isCollected = true;
      state.addSun(this.amount);
      this.timeToLive = 0; // تیک بعدی پاک می‌شود
    }
  }

  public boolean isExpired() {
    return timeToLive <= 0 || isCollected;
  }

  public int getAmount() {
    return amount;
  }

  public int getTimeToLive() {
    return timeToLive;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public SunType getType() {
    return this.sunType;
  }

  public boolean isFalling() {
    return this.fallingTicks > 0;
  }

  public void setCollected(boolean collected) {
    this.isCollected = collected;
    if (collected) {
      this.timeToLive = 0;
    }
  }
}