package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertTrue;

import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import java.util.ArrayList;
import java.util.List;
import model.game.zombie.ZombieParts.ZombieTemplate;
import org.junit.jupiter.api.Test;
import view.gdx.core.GdxConfig;

/**
 * A response to "the zombies feel fast": measures how many seconds every zombie in Zombies.json
 * actually takes to cross a nine-tile lawn, given the exact conversion ZombieFactory uses.
 *
 * <p>The conversion is {@code rawSpeed / ticksPerSecond} tiles per tick, and the model steps at
 * {@code ticksPerSecond} ticks per second, so the two cancel out and a zombie's tiles-per-second
 * in real time is just its raw JSON speed. Loads Zombies.json directly through JsonSerializer
 * rather than GameDataManager.initAllData(), which also restores the last session and would touch
 * the account layer for no reason a data check needs.
 *
 * <p>Doesn't touch UserManager, GameDataManager, or write anything -- DataPath only resolves a
 * path, JsonSerializer only reads.
 */
class ZombieSpeedBalanceTest {

  private static final int LAWN_WIDTH_TILES = 9;
  // Loose on purpose: this guards against a real data-entry or unit-conversion mistake (a zombie
  // crossing in 3 seconds, or in an hour), not against fine balance tuning.
  private static final double FASTEST_ACCEPTABLE_CROSSING_SECONDS = 15.0;
  private static final double SLOWEST_ACCEPTABLE_CROSSING_SECONDS = 200.0;

  /**
   * ZombieFactory keeps its own copy of this number rather than referencing GdxConfig (the
   * terminal build has no GDX dependency to reach it through), so nothing catches the two drifting
   * apart except a test that checks both by hand. A zombie speed bug is exactly what that drift
   * would look like: everyone's crossing time silently doubling or halving with no error anywhere.
   */
  private static final double ZOMBIE_FACTORY_TICKS_PER_SECOND = 10.0;

  @Test
  void zombieFactorysTickRateAgreesWithTheGraphicalBuildsTickRate() {
    assertTrue(ZOMBIE_FACTORY_TICKS_PER_SECOND == GdxConfig.TICKS_PER_SECOND,
        "ZombieFactory's own TICKS_PER_SECOND and GdxConfig.TICKS_PER_SECOND must match, or "
            + "the graphical build's real-time pacing no longer matches what ZombieFactory divided "
            + "every zombie's speed by");
  }

  @Test
  void everyZombieCrossesTheLawnInASaneAmountOfTime() {
    List<String> outOfRange = new ArrayList<>();
    for (ZombieTemplate template : loadZombieTemplates()) {
      double speed = template.getBaseSpeed(); // tiles per second, once the /10 and *10 cancel
      if (speed <= 0) {
        continue; // armor pieces and other non-movers carry no speed
      }
      double crossingSeconds = LAWN_WIDTH_TILES / speed;
      if (crossingSeconds < FASTEST_ACCEPTABLE_CROSSING_SECONDS
          || crossingSeconds > SLOWEST_ACCEPTABLE_CROSSING_SECONDS) {
        outOfRange.add(String.format("%s: speed=%.4f -> %.1fs to cross",
            firstAlias(template), speed, crossingSeconds));
      }
    }
    assertTrue(outOfRange.isEmpty(),
        "zombie(s) with an implausible crossing time: " + outOfRange);
  }

  private static String firstAlias(ZombieTemplate template) {
    return template.aliases == null || template.aliases.isEmpty() ? "?" : template.aliases.get(0);
  }

  private static List<ZombieTemplate> loadZombieTemplates() {
    String path = DataPath.getInstance().getPath("zombies").toString();
    ZombieTemplate[] templates = new JsonSerializer().readFromFile(path, ZombieTemplate[].class);
    return templates == null ? List.of() : List.of(templates);
  }
}
