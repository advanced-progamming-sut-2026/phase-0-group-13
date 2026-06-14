package model.game;

import model.enums.ZombieType;

import java.util.List;

public class Wave {
    private int waveNumber;
    private List<ZombieType> zombiesToSpawn;
    // برای دیلی موجمون باید یه هش مپ بزنیم که دیلی هرکدوم و
    private int spawnDelay;
    private int dificulity;
    private boolean isCompleted;
    private int delay ;
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
