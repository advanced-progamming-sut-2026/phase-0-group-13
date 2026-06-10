package model.game;

public class Board {
    private int rows;
    private int columns;
    private Tile[][] tiles;

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
