package model.game;

import model.XY;
import model.plant.BasePlant;
import model.zombie.BaseZombie;

import java.util.HashMap;

public class Board {
    private int rows;
    private int columns;
    private Tile[][] tiles;
    private HashMap<XY, BaseZombie> Zombies;
    private HashMap<XY, BasePlant> Plants;
    private HashMap<XY , Sun> Suns ;

    public Board(int rows, int columns) {
        this.rows = rows;
    }

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
