package model.game.TileEffects;

public class FireEffect extends TileEffect {

  public static final int DAMAGE_PER_TICK = 40;

  public FireEffect(int durationTicks) {
    super("Fire", durationTicks);
  }

  @Override
  public boolean blocksPlanting() {
    return isActive();
  }
}
