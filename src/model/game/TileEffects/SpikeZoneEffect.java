package model.game.TileEffects;

import model.game.zombie.Zombie;

import java.util.List;

public class SpikeZoneEffect extends TileEffect {
    private int damagePerTick;
    private int tickInterval;

    public SpikeZoneEffect(int duration, int damagePerTick, int tickInterval) {
        super("Spike Zone", duration);
        this.damagePerTick = damagePerTick;
        this.tickInterval = tickInterval;
    }

    public void applyDamageToZombies(List<Zombie> zombiesOnTile, int currentTick) {
        if (!isActive()) return;

        if (currentTick % tickInterval == 0) {
            for (Zombie zombie : zombiesOnTile) {
                if (zombie != null && zombie.getCurrentHealth() > 0) {
                    zombie.takeDamage(damagePerTick, false);
                }
            }
        }
    }
}