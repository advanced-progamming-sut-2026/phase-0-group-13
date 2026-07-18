package model.game;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.core.MatchSetup;
import model.game.zombie.ZombieParts.ZombieTemplate;

public class WaveGenerator {
  private static final int TICKS_PER_SECOND = 10;
  private static final int MAX_WAVES = 10;
  private static final int SPAWN_SPACING_SECONDS = 3;
  private static final int DEFAULT_LANES = 5;
  // WavePointCost تو دیتای واقعی بین ۱۰۰ تا ۱۵۰۰ پخش شده (میانگین ~۴۰۰)؛ بودجه پایه رو طوری گذاشتیم
  // که موج اول معمولا ۲ تا ۴ زامبی داشته باشه، نه فقط یکی
  private static final int DEFAULT_WAVE_COST = 500;
  private static final int FALLBACK_ZOMBIE_COST = 100;
  private static final double DIFFICULTY_GROWTH_PER_WAVE = 0.25;
  private static final double FINAL_WAVE_MULTIPLIER = 2.0;
  private static final int MAX_ZOMBIES_PER_WAVE = 60;

  private WaveGenerator() {}

  public static List<Wave> generate(int levelNumber, List<String> availableZombieNames) {
    return generateInternal(levelNumber, availableZombieNames, new Random());
  }

  public static List<Wave> generateDailyScoreGameWaves(int levelNumber, List<String> availableZombieNames) {
    long dailySeed = System.currentTimeMillis() / 86400000L;
    Random dailyRandom = new Random(dailySeed);
    return generateInternal(levelNumber, availableZombieNames, dailyRandom);
  }

  private static List<Wave> generateInternal(
          int levelNumber, List<String> availableZombieNames, Random random) {
    List<Wave> waves = new ArrayList<>();
    if (availableZombieNames == null || availableZombieNames.isEmpty()) {
      return waves;
    }

    int waveCount = Math.min(MAX_WAVES, 3 + levelNumber / 2);
    double levelMultiplier = levelDifficultyMultiplier(levelNumber);

    double waveDifficulty = 1.0;
    for (int waveNumber = 1; waveNumber <= waveCount; waveNumber++) {
      boolean isFinalWave = waveNumber == waveCount;

      // هر موج نسبت به موج قبل ۲۵٪ سخت‌تره؛ موج آخر (پرچم) استثنا: به‌جای ادامه تصاعد، ۲ برابر موج
      // قبل از خودشه (قرارداد معمول wave های پرچم)
      if (waveNumber == 1) {
        waveDifficulty = 1.0;
      } else if (isFinalWave) {
        waveDifficulty = waveDifficulty * FINAL_WAVE_MULTIPLIER;
      } else {
        waveDifficulty = waveDifficulty * (1.0 + DIFFICULTY_GROWTH_PER_WAVE);
      }

      int waveBudget = (int) Math.round(DEFAULT_WAVE_COST * levelMultiplier * waveDifficulty);
      waves.add(buildWave(waveNumber, isFinalWave, waveBudget, availableZombieNames, random));
    }

    return waves;
  }

  // MatchSetup.getDifficultyLevel() معمولا ۱ تا ۵ هست (پیش‌فرض ۳ = متوسط)؛ هر پله فاصله از ۳، ۱۵٪
  // رو بودجه‌ی موج اثر میذاره. levelNumber هم خودش رو تصاعد بین موج‌ها اثر داره.
  private static double levelDifficultyMultiplier(int levelNumber) {
    int difficultyLevel = MatchSetup.getInstance().getDifficultyLevel();
    double difficultyFactor = 1.0 + (difficultyLevel - 3) * 0.15;
    double levelFactor = 1.0 + Math.max(0, levelNumber - 1) * 0.05;
    return Math.max(0.25, difficultyFactor) * levelFactor;
  }

  // به‌جای انتخاب تعداد ثابت زامبی، یه بودجه (waveCost) داریم و تا وقتی بودجه تموم نشده زامبی رندوم
  // از استخر انتخاب میکنیم (هرکدوم WavePointCost خودشو از دیتا مصرف میکنه)
  private static Wave buildWave(
          int waveNumber, boolean isFinalWave, int budget, List<String> pool, Random random) {
    List<Wave.SpawnEntry> spawns = new ArrayList<>();
    int remainingBudget = budget;
    int spawnedCount = 0;

    while (remainingBudget > 0 && spawnedCount < MAX_ZOMBIES_PER_WAVE) {
      String zombieName = pool.get(random.nextInt(pool.size()));
      int cost = resolveWaveCost(zombieName);

      if (cost > remainingBudget && spawnedCount > 0) {
        break;
      }

      int lane = random.nextInt(DEFAULT_LANES);
      int delay = (spawnedCount / 2) * SPAWN_SPACING_SECONDS * TICKS_PER_SECOND;
      spawns.add(new Wave.SpawnEntry(zombieName, lane, delay, cost));

      remainingBudget -= cost;
      spawnedCount++;
    }

    return new Wave(waveNumber, isFinalWave, spawns);
  }

  private static int resolveWaveCost(String zombieName) {
    if (GameDataManager.zombieRepository == null) {
      return FALLBACK_ZOMBIE_COST;
    }
    ZombieTemplate template = GameDataManager.zombieRepository.find(zombieName);
    if (template == null) {
      return FALLBACK_ZOMBIE_COST;
    }
    int cost = template.getWavePointCost();
    return cost > 0 ? cost : FALLBACK_ZOMBIE_COST;
  }
}
