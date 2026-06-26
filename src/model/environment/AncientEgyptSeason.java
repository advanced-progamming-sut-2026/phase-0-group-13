package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class AncientEgyptSeason extends Season {
  public AncientEgyptSeason() {
    this.name = "Ancient Egypt";
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
