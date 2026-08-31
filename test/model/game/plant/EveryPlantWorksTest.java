package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Sweeps the whole roster rather than sampling it: every plant in plants.json is built, put on a
 * board with a zombie walking into it, and required to actually do something.
 *
 * <p>Worth having as a sweep because the failure it guards against is silent. A plant whose ability
 * string stops matching what PlantFactory parses does not throw -- it comes out with no behaviour
 * and simply sits there, and the only way to notice is to plant it and wait. Nothing else in the
 * suite would catch a plant quietly becoming decorative.
 *
 * <p>The scenario matters as much as the sweep. An earlier version of this parked a stationary
 * zombie two tiles away and called 28 plants inert, all of them wrongly: melee plants and the armed
 * mines never got touched, and the sun producers were being checked against the sun counter when
 * what they actually do is drop a Sun on the board to be collected. The zombie here starts adjacent
 * and advances, and sun is counted where it lands.
 */
class EveryPlantWorksTest {

  private static List<PlantTemplate> roster;
  private static PlantFactory factory;

  @BeforeAll
  static void loadTheGameData() {
    // Reads src/data/database via DataPath; no account, network or GL involved.
    new GameDataManager();
    roster = GameDataManager.plantRepository == null
        ? List.of() : GameDataManager.plantRepository.getAll();
    factory = new PlantFactory(GameDataManager.plantRepository);
  }

  @Test
  void thePlantRosterLoaded() {
    assertTrue(roster.size() > 60,
        "expected the full plant roster, found " + roster.size() + "; the data moved");
  }

  @Test
  void everyPlantBuildsWithHealth() {
    List<String> broken = new ArrayList<>();
    for (PlantTemplate template : roster) {
      Plant plant;
      try {
        plant = factory.createPlant(template.name, 2, 2);
      } catch (RuntimeException e) {
        broken.add(template.name + " threw " + e.getClass().getSimpleName());
        continue;
      }
      if (plant == null) {
        broken.add(template.name + " came back null");
      } else if (plant.getMaxHealth() <= 0) {
        broken.add(template.name + " has no health");
      }
    }
    assertTrue(broken.isEmpty(), "plants that will not build: " + broken);
  }

  @Test
  void everyPlantDoesSomethingWhenAZombieWalksIntoIt() {
    List<String> inert = new ArrayList<>();
    for (PlantTemplate template : roster) {
      if (!actsOnce(template.name)) {
        inert.add(template.name);
      }
    }
    assertTrue(inert.isEmpty(),
        "these plants sat through a zombie walking into them without shooting, damaging, "
            + "dropping sun, changing the board or spending themselves: " + inert);
  }

  /**
   * True if this plant visibly did anything in twenty seconds of a zombie closing on it.
   *
   * <p>Deliberately broad about what counts, because the roster does very different jobs: a
   * shooter fires, a mine spends itself, a producer drops sun, a modifier changes the board, and a
   * wall's entire contribution is losing health slowly. Any of those is the plant working.
   */
  private static boolean actsOnce(String plantName) {
    Board board = new Board(5, 9);
    Plant plant = factory.createPlant(plantName, 2, 2);
    if (plant == null) {
      return false;
    }
    board.placePlant(plant);

    Zombie zombie = new Zombie("AuditTarget", 100_000, 0.08, 2, 3.2, new StandardZombieAction(20));
    board.spawnZombie(zombie);

    int zombieHealth = zombie.getCurrentHealth();
    int plantHealth = plant.getCurrentHealth();
    int plantCount = board.getPlants().size();

    for (int tick = 1; tick <= 400; tick++) {
      board.updateAll(tick);
      if (!board.getProjectiles().isEmpty() || !board.getSuns().isEmpty()) {
        return true;
      }
      if (plant.isDead() || board.getZombies().isEmpty()) {
        return true;
      }
    }
    return zombie.getCurrentHealth() != zombieHealth
        || plant.getCurrentHealth() != plantHealth
        || board.getPlants().size() != plantCount;
  }
}
