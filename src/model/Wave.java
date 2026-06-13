package model;

import model.enums.ZombieType;

import java.util.List;

public class Wave {
    private int waveNumber;
    private List<ZombieType> zombiesToSpawn;
    private int spawnDelay;
    private boolean isCompleted;
    public Wave(int waveNumber, int spawnDelay) {
        this.waveNumber = waveNumber;
        this.spawnDelay = spawnDelay;
        this.isCompleted = false;
    }

    public void startWave() {
    }

    public boolean checkCompletion() {
        return isCompleted;
    }
}
