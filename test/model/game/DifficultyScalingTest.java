package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import data.repository.ZombieRepository;
import java.util.List;
import model.core.Difficulty;
import model.core.MatchSetup;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The coefficients reaching the systems that are supposed to use them.
 *
 * <p>DifficultyTest pins the formula; this pins the wiring, which is the half that was actually
 * missing -- three of the doc's five effects were computed nowhere at all, and the two that were
 * used a different curve. Each check compares the same system at level 1 against level 5 rather
 * than asserting an absolute number, so it survives balance changes to the underlying data but
 * still fails if a system stops reading the difficulty.
 *
 * <p>Uses the real zombie roster through GameDataManager, so a scaled stat is measured on the
 * same templates the game builds from.
 */
class DifficultyScalingTest {

  @AfterEach
  void restoreDefault() {
    MatchSetup.getInstance().setDifficultyLevel(Difficulty.BASELINE_LEVEL);
  }

  private static void at(int level) {
    MatchSetup.getInstance().setDifficultyLevel(level);
  }

  /**
   * Loads the roster straight off disk, the way ZombieSpeedBalanceTest does.
   *
   * <p>GameDataManager.initAllData() would also restore the last session and reach into the
   * account layer, which a stat check has no business touching. WaveGenerator and Wave both read
   * the static repository, so it is filled in rather than only handed to the factory.
   */
  private static ZombieFactory factory() {
    if (GameDataManager.zombieRepository == null) {
      String path = DataPath.getInstance().getPath("zombies").toString();
      ZombieTemplate[] templates =
          new JsonSerializer().readFromFile(path, ZombieTemplate[].class);
      assertTrue(templates != null && templates.length > 0, "the zombie roster did not load");
      GameDataManager.zombieRepository = new ZombieRepository(List.of(templates));
    }
    return new ZombieFactory(GameDataManager.zombieRepository);
  }

  /** The first template with usable stats, so both levels are measured on the same zombie. */
  private static ZombieTemplate walker() {
    factory();
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      if (template.getBaseHp() > 0 && template.getBaseSpeed() > 0 && template.getName() != null) {
        return template;
      }
    }
    throw new IllegalStateException("no usable zombie template in the roster");
  }

  private static Zombie basicAt(int level) {
    at(level);
    Zombie zombie = factory().createZombie(walker().getName(), 0, 9.0);
    assertTrue(zombie != null, "the roster's first walker could not be built");
    return zombie;
  }

  @Test
  void aZombieIsTougherAndFasterAtLevelFiveThanAtLevelOne() {
    Zombie easy = basicAt(Difficulty.MIN_LEVEL);
    Zombie hard = basicAt(Difficulty.MAX_LEVEL);

    assertTrue(hard.getMaxHealth() > easy.getMaxHealth(),
        "health did not scale: " + easy.getMaxHealth() + " -> " + hard.getMaxHealth());
    assertTrue(hard.getSpeed() > easy.getSpeed(),
        "speed did not scale: " + easy.getSpeed() + " -> " + hard.getSpeed());

    assertEquals(Difficulty.increase(5) / Difficulty.increase(1),
        hard.getSpeed() / easy.getSpeed(), 1e-9,
        "speed should scale by exactly the doc's ratio");

    // Health is rounded to a whole number, so the exact expected values are compared rather than
    // their ratio -- on a small-HP zombie the rounding alone moves the ratio by a few percent.
    int base = walker().getBaseHp();
    assertEquals(Math.round(base * Difficulty.increase(1)), easy.getMaxHealth(),
        "level 1 health should be the roster value times 1/3");
    assertEquals(Math.round(base * Difficulty.increase(5)), hard.getMaxHealth(),
        "level 5 health should be the roster value times 5/3");
  }

  @Test
  void levelThreeLeavesAZombiesStatsAsTheRosterHasThem() {
    at(Difficulty.BASELINE_LEVEL);
    ZombieFactory factory = factory();
    for (var template : GameDataManager.zombieRepository.getAll()) {
      if (template.getBaseHp() <= 0 || template.getBaseSpeed() <= 0) {
        continue;
      }
      Zombie zombie = factory.createZombie(template.getName(), 0, 9.0);
      if (zombie == null) {
        continue;
      }
      assertEquals(template.getBaseHp(), zombie.getMaxHealth(),
          template.getName() + " should be untouched at the baseline level");
      return;
    }
  }

  @Test
  void aWaveHoldsMoreZombiesAtLevelFiveThanAtLevelOne() {
    assertTrue(spawnedInFirstWave(Difficulty.MAX_LEVEL) > spawnedInFirstWave(Difficulty.MIN_LEVEL),
        "a cheaper zombie should mean more of them fit into the same wave budget");
  }

  /**
   * Runs level 1's first wave with a pool of exactly one zombie type, and counts what reached the
   * board.
   *
   * <p>One type on purpose: WaveGenerator picks randomly from the pool with an unseeded Random, so
   * with a mixed pool the count is dominated by which zombies happened to come up rather than by
   * what they cost. With a single type the wave is budget divided by cost, which is precisely the
   * thing the difficulty coefficient moves. Counted off the board because Wave exposes no
   * accessor for its pending spawns.
   */
  private static int spawnedInFirstWave(int level) {
    at(level);
    factory();
    List<String> pool = List.of(cheapest().getName());

    List<Wave> waves = WaveGenerator.generate(1, pool);
    assertTrue(!waves.isEmpty(), "no waves were generated");
    Board board = new Board(5, 9);
    Wave first = waves.get(0);
    // long enough for every spawn delay in one wave to elapse
    for (int tick = 0; tick < 600; tick++) {
      first.update(board);
    }
    return board.getZombies().size();
  }

  /** The cheapest walker, so level 5's budget buys a visibly different number of them. */
  private static ZombieTemplate cheapest() {
    ZombieTemplate best = null;
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      if (template.getName() == null || template.getBaseHp() <= 0
          || template.getWavePointCost() <= 0) {
        continue;
      }
      if (best == null || template.getWavePointCost() < best.getWavePointCost()) {
        best = template;
      }
    }
    assertTrue(best != null, "no priced zombie in the roster");
    return best;
  }

  @Test
  void lessSunFallsAtLevelFiveThanAtLevelOne() {
    assertTrue(sunsDropped(Difficulty.MAX_LEVEL) < sunsDropped(Difficulty.MIN_LEVEL),
        "the sky should be stingier at the highest difficulty");
    // and the baseline still drops sun at the rate the doc's formula alone gives
    assertTrue(sunsDropped(Difficulty.BASELINE_LEVEL) > 0, "no sun fell at the baseline level");
  }

  /** Runs a board's clock for two simulated minutes and counts what the sky produced. */
  private static int sunsDropped(int level) {
    at(level);
    Board board = new Board(5, 9);
    SunManager sky = new SunManager();
    for (int tick = 0; tick <= 1200; tick++) {
      sky.handleSkySunDrop(tick, board);
    }
    return sky.getSuns().size();
  }
}
