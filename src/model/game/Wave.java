package model.game;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

public class Wave {

  public static class SpawnEntry {
    private final String zombieName;
    private final int lane;
    private final int spawnDelayTicks;
    private final int waveCost;

    public SpawnEntry(String zombieName, int lane, int spawnDelayTicks, int waveCost) {
      this.zombieName = zombieName;
      this.lane = lane;
      this.spawnDelayTicks = spawnDelayTicks;
      this.waveCost = waveCost;
    }
  }

  private final int waveNumber;
  private final boolean finalWave;
  private final List<SpawnEntry> pendingSpawns;
  private final List<Zombie> spawnedZombies = new ArrayList<>();

  private int ticksSinceStart = 0;
  private boolean started = false;
  private boolean allSpawned = false;
  private boolean completed = false;
  private double totalStartingHealth = 0;

  public Wave(int waveNumber, boolean finalWave, List<SpawnEntry> spawns) {
    this.waveNumber = waveNumber;
    this.finalWave = finalWave;
    this.pendingSpawns = new ArrayList<>(spawns);
    if (spawns.isEmpty()) {
      this.allSpawned = true;
    }
  }

  public void update(Board board) {
    if (completed) return;

    if (!started) {
      started = true;
      System.out.printf("Wave %d started.%n", waveNumber);
      if (finalWave) {
        System.out.println("The final wave has come.");
      }
    }

    ticksSinceStart++;

    ZombieFactory zombieFactory = new ZombieFactory(GameDataManager.zombieRepository);
    Iterator<SpawnEntry> iterator = pendingSpawns.iterator();
    while (iterator.hasNext()) {
      SpawnEntry entry = iterator.next();
      if (ticksSinceStart >= entry.spawnDelayTicks) {
        int lane = board.getRows() > 0 ? entry.lane % board.getRows() : entry.lane;
        Zombie zombie = zombieFactory.createZombie(entry.zombieName, lane, board.getColumns());

        if (zombie != null) {
          board.spawnZombie(zombie);
          spawnedZombies.add(zombie);
          totalStartingHealth += zombie.getMaxHealth();
          System.out.printf(
              "Zombie %s spawned at wave %d in lane %d which costed %d.%n",
              entry.zombieName, waveNumber, lane + 1, entry.waveCost);
        }
        iterator.remove();
      }
    }

    if (pendingSpawns.isEmpty()) {
      allSpawned = true;
    }

    // اسپک: موج بعدی وقتی شروع میشه که ۷۵٪ از کل جونی که این موج اسپاون کرده از بین رفته باشه (نه
    // وقتی همه‌ش اسپاون شدن)
    if (allSpawned && totalStartingHealth > 0) {
      double remainingHealth = 0;
      for (Zombie zombie : spawnedZombies) {
        remainingHealth += Math.max(0, zombie.getCurrentHealth());
      }
      double killedFraction = 1.0 - (remainingHealth / totalStartingHealth);
      if (killedFraction >= 0.75) {
        completed = true;
      }
    } else if (allSpawned && spawnedZombies.isEmpty()) {
      // هیچ زامبی‌ای واقعا اسپاون نشد (مثلا استخر خالی بود)؛ گیر نکنیم
      completed = true;
    }
  }

  public boolean checkCompletion() {
    return completed;
  }

  public int getWaveNumber() {
    return waveNumber;
  }

  public boolean isFinalWave() {
    return finalWave;
  }
}
