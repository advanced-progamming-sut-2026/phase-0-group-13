package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Every shot a plant puts on the board says which plant it came from.
 *
 * <p>The view picks a shot's artwork by that name, out of a {@link java.util.Map#ofEntries} -- and
 * an immutable map throws on a null key where a {@link java.util.HashMap} would simply miss. A
 * Grapeshot's blast scatters six ricocheting grapes and named none of them, so the first frame
 * after it went off took the render thread down with it and the game closed.
 */
class ProjectileSourceTest {

  private static PlantFactory plants;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    plants = new PlantFactory(GameDataManager.plantRepository);
  }

  /** Every projectile this plant creates over its first few seconds, named or not. */
  private static List<Projectile> shotsFrom(String plantName, int ticks) {
    Board board = new Board(5, 9);
    Plant plant = plants.createPlant(plantName, 2, 2);
    assertNotNull(plant, plantName + " did not build");
    board.placePlant(plant);
    board.spawnZombie(new Zombie("Target", 500_000, 0.02, 2, 7.0, new StandardZombieAction(20)));

    Set<Projectile> seen = new LinkedHashSet<>();
    for (int tick = 1; tick <= ticks; tick++) {
      board.updateAll(tick);
      seen.addAll(board.getProjectiles());
    }
    return new ArrayList<>(seen);
  }

  @Test
  void aGrapeshotNamesTheGrapesItScatters() {
    List<Projectile> grapes = shotsFrom("Grapeshot", 60);
    assertFalse(grapes.isEmpty(), "the blast scattered nothing at all");
    for (Projectile grape : grapes) {
      assertEquals("Grapeshot", grape.getSourceName(),
          "a scattered grape with no plant on it is what the view cannot look up");
    }
  }

  @Test
  void noPlantOnTheRosterFiresSomethingUnnamed() {
    List<String> anonymous = new ArrayList<>();
    for (var template : GameDataManager.plantRepository.getAll()) {
      Plant plant = plants.createPlant(template.name, 2, 2);
      if (plant == null || plant.getBehavior() == null) {
        continue;
      }
      for (Projectile shot : shotsFrom(template.name, 60)) {
        if (!shot.isFromZombie() && shot.getSourceName() == null) {
          anonymous.add(template.name);
          break;
        }
      }
    }
    assertTrue(anonymous.isEmpty(),
        "these plants put a shot on the lawn with no name for the view to draw it by: "
            + anonymous);
  }
}
