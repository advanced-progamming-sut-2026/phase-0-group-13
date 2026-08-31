package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.Board;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.factory.ZombieFactory;
import model.game.zombie.ZombieParts.ZombieTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Sweeps the whole playable zombie roster, the same way {@link
 * model.game.plant.EveryPlantWorksTest} sweeps plants: every zombie is built, put on a board with
 * a wall-nut two tiles ahead, and required to actually affect either the plant or itself within a
 * long tick budget.
 *
 * <p>{@link data.repository.ZombieRepository#getAlmanacEntries()} is the roster, not {@code
 * getAll()} -- that also returns armour-definition entries and scenery items that share the same
 * JSON file but are not zombies a wave ever spawns, and it is deduplicated by {@link
 * model.enums.ZombieType} so a chapter's four armour skins of the same zombie are not counted four
 * times.
 *
 * <p>"Affects" is deliberately broad, because "eats a wall" is not what most of this roster does:
 * a laser or siphon damages or drains without ever setting isEating, an octopus disables the plant
 * instead of damaging it, a wizard curses it, ice freezes it in stages that read as no visible
 * change until the third hit, and several are just slow. The budget was set by measuring the roster
 * ahead of time rather than guessed: the slowest playable zombie (a Zombotany wall-nut head) takes
 * about 89 seconds to close 8 tiles at its own speed, so 140 seconds of ticks covers all of them
 * with room to spare. A shorter budget flagged four zombies as broken here during development, and
 * all four turned out to be exactly this slow and nothing else.
 */
class EveryZombieWorksTest {

  private static final int TICK_BUDGET = 1400;

  private static List<ZombieTemplate> roster;
  private static ZombieFactory zombieFactory;
  private static PlantFactory plantFactory;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    roster = GameDataManager.zombieRepository == null
        ? List.of() : GameDataManager.zombieRepository.getAlmanacEntries();
    zombieFactory = new ZombieFactory(GameDataManager.zombieRepository);
    plantFactory = new PlantFactory(GameDataManager.plantRepository);
  }

  @Test
  void theZombieRosterLoaded() {
    assertTrue(roster.size() > 25,
        "expected the full playable zombie roster, found " + roster.size() + "; the data moved");
  }

  @Test
  void everyZombieBuildsWithHealth() {
    List<String> broken = new ArrayList<>();
    for (ZombieTemplate template : roster) {
      Zombie zombie;
      try {
        zombie = zombieFactory.createZombie(template.getName(), 2, 8.0);
      } catch (RuntimeException e) {
        broken.add(template.getName() + " threw " + e.getClass().getSimpleName());
        continue;
      }
      if (zombie == null) {
        broken.add(template.getName() + " came back null");
      } else if (zombie.getMaxHealth() <= 0) {
        broken.add(template.getName() + " has no health");
      }
    }
    assertTrue(broken.isEmpty(), "zombies that will not build: " + broken);
  }

  @Test
  void everyZombieAffectsTheWallOrItself() {
    List<String> inert = new ArrayList<>();
    for (ZombieTemplate template : roster) {
      if (!actsWithinBudget(template.getName())) {
        inert.add(template.getName());
      }
    }
    assertTrue(inert.isEmpty(),
        "these zombies neither touched the wall-nut ahead of them nor changed their own state "
            + "in " + (TICK_BUDGET / 10) + " simulated seconds: " + inert);
  }

  private static boolean actsWithinBudget(String zombieName) {
    Board board = new Board(5, 9);
    Zombie zombie = zombieFactory.createZombie(zombieName, 2, 8.0);
    if (zombie == null) {
      return false;
    }
    board.spawnZombie(zombie);

    Plant wall = plantFactory.createPlant("wall-nut", 2, 0);
    board.placePlant(wall);
    board.getGameState().addSun(1000); // something for a sun-siphon to actually steal

    int zombieHealthBefore = zombie.getCurrentHealth();
    int plantHealthBefore = wall.getCurrentHealth();
    int sunBefore = board.getGameState().getCurrentSun();

    for (int tick = 1; tick <= TICK_BUDGET; tick++) {
      board.updateAll(tick);
      Plant currentWall = board.getPlantAt(2, 0);
      boolean plantRemoved = currentWall == null;
      boolean plantDamaged = !plantRemoved && currentWall.getCurrentHealth() != plantHealthBefore;
      boolean plantCursed = !plantRemoved && currentWall.isCursed();
      boolean plantHeld = !plantRemoved && currentWall.isHeldByOctopus(tick);
      boolean plantIced = !plantRemoved
          && (currentWall.getFreezeLevel() > 0 || currentWall.isFrozen(tick));

      if (zombie.isEating() || plantRemoved || plantDamaged || plantCursed || plantHeld
          || plantIced
          || zombie.getCurrentHealth() != zombieHealthBefore
          || board.getGameState().getCurrentSun() != sunBefore
          || board.getZombies().size() != 1
          || zombie.isDead()) {
        return true;
      }
    }
    return false;
  }
}
