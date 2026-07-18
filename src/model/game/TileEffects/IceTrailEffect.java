package model.game.TileEffects;

public class IceTrailEffect extends TileEffect {
  private double speedMultiplier;
  private final boolean fullFreeze;

  public IceTrailEffect(int duration, double speedMultiplier) {
    this(duration, speedMultiplier, false);
  }

  // fullFreeze=true یعنی "تایل یخ‌زده" (زامبی رو کاملا فریز میکنه، مثل Frostbite Caves)؛
  // fullFreeze=false همون "تایل لیزخوردن" قبلیه (فقط کند میکنه)
  public IceTrailEffect(int duration, double speedMultiplier, boolean fullFreeze) {
    super("Ice Trail", duration);
    this.speedMultiplier = speedMultiplier;
    this.fullFreeze = fullFreeze;
  }

  public boolean canPlantHere() {
    return !isActive();
  }

  public boolean isFullFreeze() {
    return fullFreeze;
  }

  public double getSpeedMultiplier() {
    return isActive() ? speedMultiplier : 1.0;
  }
}
