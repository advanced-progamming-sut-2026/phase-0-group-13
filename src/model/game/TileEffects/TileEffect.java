package model.game.TileEffects;

public class TileEffect {
    private String name;
    private int duration;
    private boolean active;


    public void apply() {
        active = true;
    }

    public void tick() {
    }

    public void remove() {
        active = false;
    }
}
