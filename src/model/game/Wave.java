package model.game;

import model.enums.ZombieType;

import java.util.List;

public class Wave {
    private int waveNumber;
    private List<ZombieType> zombiesToSpawn;
    private int spawnDelay;
    private int dificulity ;
    private boolean isCompleted;
    private int Delay ;
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
