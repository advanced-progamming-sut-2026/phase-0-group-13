package model.environment.greenhouse;

public class Pot {
  private boolean isUnlocked;
  private String plantedSeedId;
  private long plantTime;
  private long growthDurationMillis;

  public static final long MARIGOLD_GROWTH = 2L * 60 * 60 * 1000;
  public static final long RANDOM_GROWTH = 8L * 60 * 60 * 1000;

  public Pot(boolean isUnlocked) {
    this.isUnlocked = isUnlocked;
    this.plantedSeedId = null;
    this.plantTime = 0;
    this.growthDurationMillis = 0;
  }

  public void unlock() {
    this.isUnlocked = true;
  }

  public void plant(String seedId, long durationMillis) {
    if (!isUnlocked) return;
    this.plantedSeedId = seedId;
    this.growthDurationMillis = durationMillis;
    this.plantTime = System.currentTimeMillis();
  }

  public void collect() {
    this.plantedSeedId = null;
    this.plantTime = 0;
    this.growthDurationMillis = 0;
  }

  public long getElapsedGrowTime() {
    if (isEmpty()) return 0;
    return System.currentTimeMillis() - plantTime;
  }

  public long getRemainingGrowTime() {
    if (isEmpty()) return 0;
    long remaining = growthDurationMillis - getElapsedGrowTime();
    return Math.max(0, remaining);
  }

  public double getGrowthProgress() {
    if (isEmpty() || growthDurationMillis <= 0) return 0.0;
    double progress = (double) getElapsedGrowTime() / (double) growthDurationMillis;
    return Math.max(0.0, Math.min(1.0, progress));
  }

  public boolean isFullyGrown() {
    return !isEmpty() && getElapsedGrowTime() >= growthDurationMillis;
  }

  public void forceFinishGrowth() {
    if (isEmpty()) return;
    this.plantTime = System.currentTimeMillis() - growthDurationMillis;
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
}
