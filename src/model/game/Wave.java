package model.game;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;
import data.GameDataManager;

public class Wave {
    private int waveNumber;
    private List<String> zombiesToSpawn;
    private Map<String, Integer> spawnDelays; // همه زامبی هارو با تاخیرشون تو اینجا داریم
    private int ticksSinceStart;
    private boolean isCompleted;

    public Wave(int waveNumber, List<String> zombiesToSpawn, Map<String, Integer> spawnDelays) {
        this.waveNumber = waveNumber;
        this.zombiesToSpawn = new ArrayList<>(zombiesToSpawn);
        this.spawnDelays = spawnDelays;
        this.ticksSinceStart = 0;
        this.isCompleted = false;
    }

    public void update(Board board) {
        if (isCompleted) return;

        ticksSinceStart++;
        Iterator<String> iterator = zombiesToSpawn.iterator();

        ZombieFactory zombieFactory = new ZombieFactory(GameDataManager.zombieRepository);

        while (iterator.hasNext()) {
            String zombieName = iterator.next();
            int requiredDelay = spawnDelays.getOrDefault(zombieName.toLowerCase(), 0);

            if (ticksSinceStart >= requiredDelay) {
                int randomRow = new java.util.Random().nextInt(board.getRows());

                Zombie newZombie = zombieFactory.createZombie(zombieName, randomRow, 9.0);

                if (newZombie != null) {
                    board.spawnZombie(newZombie);
                }
                iterator.remove();
            }
        }

        if (zombiesToSpawn.isEmpty()) {
            isCompleted = true;
        }
    }

    public boolean checkCompletion() {
        return isCompleted;
    }
}