package model.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WaveGenerator {
  private static final int TICKS_PER_SECOND = 10;
  private static final int MAX_WAVES = 10;
  private static final int SPAWN_SPACING_SECONDS = 3;

  private WaveGenerator() {}

  // فرمول اولیه: هرچی levelNumber بالاتر بره هم تعداد موج‌ها بیشتر میشه هم تعداد زامبی هر موج، و موج
  // آخر (موج پرچم) ۵۰٪ سنگین‌تر از بقیه‌س. چون Wave.spawnDelays فقط یه delay برای هر اسم زامبی نگه
  // میداره (نه هر نمونه جداگانه)، تا جایی که ممکنه از تکرار یه اسم تو یه موج پرهیز میکنیم؛ وقتی
  // استخر زامبی از تعداد لازم کوچیک‌تر باشه، تکرار پیش میاد و همه‌ی نمونه‌های اون اسم با هم روی یه
  // delay اسپاون میشن (محدودیت خود Wave، نه این جنریتور).
  public static List<Wave> generate(int levelNumber, List<String> availableZombieNames) {
    List<Wave> waves = new ArrayList<>();
    if (availableZombieNames == null || availableZombieNames.isEmpty()) {
      return waves;
    }

    int waveCount = Math.min(MAX_WAVES, 3 + levelNumber / 2);
    Random random = new Random();

    for (int waveNumber = 1; waveNumber <= waveCount; waveNumber++) {
      boolean isFlagWave = waveNumber == waveCount;
      int zombieCount = 3 + levelNumber + waveNumber;
      if (isFlagWave) {
        zombieCount = (int) Math.round(zombieCount * 1.5);
      }

      waves.add(buildWave(waveNumber, zombieCount, availableZombieNames, random));
    }

    return waves;
  }

  private static Wave buildWave(
      int waveNumber, int zombieCount, List<String> pool, Random random) {
    List<String> shuffledPool = new ArrayList<>(pool);
    java.util.Collections.shuffle(shuffledPool, random);

    List<String> zombiesToSpawn = new ArrayList<>();
    Map<String, Integer> spawnDelays = new HashMap<>();

    for (int i = 0; i < zombieCount; i++) {
      String zombieName =
          i < shuffledPool.size() ? shuffledPool.get(i) : pool.get(random.nextInt(pool.size()));
      zombiesToSpawn.add(zombieName);

      int delay = (i / 2) * SPAWN_SPACING_SECONDS * TICKS_PER_SECOND;
      spawnDelays.merge(zombieName.toLowerCase(), delay, Math::max);
    }

    return new Wave(waveNumber, zombiesToSpawn, spawnDelays);
  }
}
