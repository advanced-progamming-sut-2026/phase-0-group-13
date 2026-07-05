package model.environment.greenhouse;

public class Pot {
  private boolean isUnlocked;
  private String plantedSeedId;
  private long plantTime;

  public static final long DEFAULT_GROWTH_DURATION_MILLIS = 30L * 60 * 1000;

  public Pot(boolean isUnlocked) {
    this.isUnlocked = isUnlocked;
    this.plantedSeedId = null;
    this.plantTime = 0;
  }

  public void unlock() {
    this.isUnlocked = true;
  }

  public void plant(String seedId) {
    if (!isUnlocked) return;
    this.plantedSeedId = seedId;
    this.plantTime = System.currentTimeMillis();
  }

  public void collect() {
    this.plantedSeedId = null;
    this.plantTime = 0;
  }

  public long getElapsedGrowTime() {
    if (isEmpty()) return 0;
    return System.currentTimeMillis() - plantTime;
  }

  public long getRemainingGrowTime(long growthDurationMillis) {
    if (isEmpty()) return 0;
    long remaining = growthDurationMillis - getElapsedGrowTime();
    return Math.max(0, remaining);
  }

  public long getRemainingGrowTime() {
    return getRemainingGrowTime(DEFAULT_GROWTH_DURATION_MILLIS);
  }

  public double getGrowthProgress(long growthDurationMillis) {
    if (isEmpty() || growthDurationMillis <= 0) return 0.0;
    double progress = (double) getElapsedGrowTime() / (double) growthDurationMillis;
    return Math.max(0.0, Math.min(1.0, progress));
  }

  public double getGrowthProgress() {
    return getGrowthProgress(DEFAULT_GROWTH_DURATION_MILLIS);
  }

  public boolean isFullyGrown(long growthDurationMillis) {
    return !isEmpty() && getElapsedGrowTime() >= growthDurationMillis;
  }

  public boolean isFullyGrown() {
    return isFullyGrown(DEFAULT_GROWTH_DURATION_MILLIS);
  }

  public void forceFinishGrowth() {
    if (isEmpty()) return;
    this.plantTime = System.currentTimeMillis() - DEFAULT_GROWTH_DURATION_MILLIS;
  }

  public boolean isUnlocked() {
    return isUnlocked;
  }

  public boolean isEmpty() {
    return plantedSeedId == null;
  }

  public String getPlantedSeedId() {
    return plantedSeedId;
  }

  public long getPlantTime() {
    return plantTime;
  }
}
