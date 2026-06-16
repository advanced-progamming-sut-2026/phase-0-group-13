package model.environment;

import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;
import view.MainMenuSubMenus.GameMenuSubMenus.MiniGames.MiniGame;

import java.util.List;

abstract public class Season {
    protected String name;
    private List<Zombie> AvailableZombies;
    private List<Stage> stages;
    private List<MiniGame> miniGames;
    public String getName() {
        return name;
    }
    public void Initialize( )
    {
        // داخل اینجا مینی گیم ها و .... اینیشالایز میکنیم
    }

    public abstract void applySeasonEffects(GameState gameState);

    public abstract List<Zombie> getAvailableZombies();

    public abstract List<Tile> generateMap();
}
