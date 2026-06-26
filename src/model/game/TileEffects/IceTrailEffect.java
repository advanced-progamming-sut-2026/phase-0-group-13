package model.game.TileEffects;

public class IceTrailEffect extends TileEffect {
  private double speedMultiplier;

  public IceTrailEffect(int duration, double speedMultiplier) {
    super("Ice Trail", duration);
    this.speedMultiplier = speedMultiplier;
  }

  public boolean canPlantHere() {
    return !isActive();
  }

  public double getSpeedMultiplier() {
    return isActive() ? speedMultiplier : 1.0;
  }
}
