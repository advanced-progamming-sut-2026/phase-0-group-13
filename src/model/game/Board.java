package model.game;

import model.game.plant.Plant;
import model.game.zombie.Zombie;

import java.util.HashMap;
import java.util.List;

public class Board {
    private int rows;
    private int columns;
    private Tile[][] tiles;
    private List<Zombie> Zombies;
    private List<Plant> Plants;
    private List<Sun> Suns ;
    private HashMap<Double, Plant> availableplants;
    private GameState gameState = new GameState();
    public GameState getGameState() {
        return gameState;
    }
    public Board(int rows, int columns) {
        this.rows = rows;
    }
    // داخل کانسترکتور ما میایم و همه دیتا هارو اینیشیالایز میکنیم و ...(گیاهایی که انتخاب کردیم و میزاریم مثلا و... )

    public void initialize() {
        tiles = new Tile[rows][columns];
    }

    public void placePlant() {
    }

    public void spawnZombie() {
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }
}
