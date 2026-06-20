package model.game;

import model.game.TileEffects.TileEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import java.util.ArrayList;
import java.util.List;

public class Tile {
    private List<Plant> plants;
    private List<Zombie> zombies;
    private List<Sun> suns;
    private TileEffect effect;

    public Tile() {
        this.plants = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.suns = new ArrayList<>();
        this.effect = null;
    }

    public void addPlant(Plant plant) {
        if (plant != null) {
            this.plants.add(plant);
        }
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) {
            this.zombies.add(zombie);
        }
    }

    public void addSun(Sun sun) {
        if (sun != null) {
            this.suns.add(sun);
        }
    }

    public void clearTile() {
        this.plants.clear();
        this.zombies.clear();
        this.suns.clear();
        this.effect = null;
    }

    public List<Plant> getPlants() { return plants; }
    public List<Zombie> getZombies() { return zombies; }
    public List<Sun> getSuns() { return suns; }
    public TileEffect getEffect() { return effect; }
    public void setEffect(TileEffect effect) { this.effect = effect; }
}