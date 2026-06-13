package model.game;

import model.XY;
import model.game.TileEffects.TileEffect;
import model.plant.BasePlant;
import model.zombie.BaseZombie;

import java.util.HashMap;

public class Tile {
    private HashMap<XY,BasePlant> plants;
    private HashMap<XY,BaseZombie> zombies;
    private HashMap<XY,Sun> Suns;
    private TileEffect effect;

    public void addPlant() {
    }

    public void addZombie() {
    }

    public void clearTile() {
        plants = null;
        zombies = null;
    }
}
