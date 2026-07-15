package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;

public class BigWaveBeachSeason extends Season {
  public BigWaveBeachSeason() {
    this.name = "Big Wave Beach";
  }

  @Override
  public void applySeasonEffects(GameState gameState) {
    gameState.update(gameState.getCurrentWave(), this);
  }

  @Override
  public List<Zombie> getAvailableZombies() {
    return zombiesByAliasKeyword("beach");
  }

  @Override
  public List<Tile> generateMap() {
    // خونه‌های آب (برای Lily Pad و گیاهان شناور) هنوز تو Tile مدل نشدن؛ فعلا شبکه معمولی برمیگردونیم
    return plainGrid();
  }
}
