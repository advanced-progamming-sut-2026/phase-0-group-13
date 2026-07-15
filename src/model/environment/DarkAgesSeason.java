package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class DarkAgesSeason extends Season {
  public DarkAgesSeason() {
    this.name = "Dark Ages";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return zombiesByAliasKeyword("dark");
  }

  @Override
  public List<Tile> generateMap() {
    return plainGrid();
  }
}
