package model.game.TileEffects;

public class CraterEffect extends TileEffect {

  public CraterEffect() {
    super("Crater", -1);
  }

  @Override
  public boolean blocksPlanting() {
    return isActive();
  }
}
