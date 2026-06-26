package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class FrostbiteCavesSeason extends Season {
  public FrostbiteCavesSeason() {
    this.name = "Frostbite Caves";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {}

  @Override
  public List<Zombie> getAvailableZombies() {
    return null;
  }

  @Override
  public List<Tile> generateMap() {
    return null;
  }
}
