package model.game.TileEffects;

public class IceTrailEffect extends TileEffect {
  private double speedMultiplier;
  private final boolean fullFreeze;
  private final int laneShift;

  public IceTrailEffect(int duration, double speedMultiplier) {
    this(duration, speedMultiplier, false, 0);
  }

  public IceTrailEffect(int duration, double speedMultiplier, boolean fullFreeze) {
    this(duration, speedMultiplier, fullFreeze, 0);
  }

  /**
   *
   * @param laneShift جهت ذاتی زمین لیز: {@code -1} لیز به بالا، {@code +1} لیز به پایین،
   * {@code 0} یعنی فقط سُر می‌خورد و ردیف عوض نمی‌کند. این جهت وسط مرحله تغییر نمی‌کند.
   */
  public IceTrailEffect(int duration, double speedMultiplier, boolean fullFreeze, int laneShift) {
    super("Ice Trail", duration);
    this.speedMultiplier = speedMultiplier;
    this.fullFreeze = fullFreeze;
    this.laneShift = laneShift;
  }

  // طبق داک روی زمین‌های لیز و یخ‌زده نمی‌توان گیاه کاشت
  @Override
  public boolean blocksPlanting() {
    return isActive();
  }

  @Override
  protected boolean clearedBy(String plantName) {
    return "Hot Potato".equalsIgnoreCase(plantName);
  }

  public int getLaneShift() {
    return laneShift;
  }

  public boolean isSlippery() {
    return laneShift != 0;
  }

  public boolean canPlantHere() {
    return !isActive();
  }

  public boolean isFullFreeze() {
    return fullFreeze;
  }

  public int getSlideDirection() {
    return laneShift;
  }

  public double getSpeedMultiplier() {
    return isActive() ? speedMultiplier : 1.0;
  }
}
