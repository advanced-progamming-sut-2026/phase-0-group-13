package model.game;

import model.game.TileEffects.TileEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

import java.util.List;

public class Tile {
    private List<Plant> plants;
    private List<Zombie> zombies;
    private List<Sun> Suns;
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
