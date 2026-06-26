package model.environment;

import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;
import view.MainMenuSubMenus.GameMenuSubMenus.MiniGames.MiniGame;

public abstract class Season {
  protected String name;
  private List<Zombie> availableZombies;
  private List<Stage> stages;
  private List<MiniGame> miniGames;

  public String getName() {
    return name;
  }

  public void initialize() {
    // داخل اینجا مینی گیم ها و .... اینیشالایز میکنیم
  }

  public abstract void applySeasonEffects(GameState gameState);

  public abstract List<Zombie> getAvailableZombies();

  public abstract List<Tile> generateMap();
}
