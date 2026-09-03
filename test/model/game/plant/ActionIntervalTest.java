package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The firing cadence the view schedules attack animations against.
 *
 * <p>The renderer plays a shooter's attack clip in the ticks leading up to its next shot so the
 * animation finishes on the frame the projectile is created, rather than starting on it. That only
 * lines up while {@link model.game.plant.behavior.PlantAction#actionIntervalTicks()} is the
 * interval the plant really fires on -- if the two drift apart the wind-up plays against nothing.
 */
class ActionIntervalTest {

  private static PlantFactory factory;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    factory = new PlantFactory(GameDataManager.plantRepository);
  }

  /** The ticks a plant actually fired on, with a target parked in front of it the whole time. */
  private static List<Integer> firingTicks(String plantName, int ticks) {
    Board board = new Board(5, 9);
    Plant plant = factory.createPlant(plantName, 2, 1);
    assertNotNull(plant, plantName + " did not build");
    board.placePlant(plant);
    board.spawnZombie(new Zombie("Target", 1_000_000, 0.0, 2, 7.0, new StandardZombieAction(20)));

    List<Integer> fired = new ArrayList<>();
    for (int tick = 1; tick <= ticks; tick++) {
      int before = plant.getLastActionTick();
      board.updateAll(tick);
      if (plant.getLastActionTick() == tick && before != tick) {
        fired.add(tick);
      }
    }
    return fired;
  }

  private static int declaredInterval(String plantName) {
    Plant plant = factory.createPlant(plantName, 2, 1);
    assertNotNull(plant);
    return plant.getBehavior() == null ? 0 : plant.getBehavior().actionIntervalTicks();
  }

  @Test
  void aShooterFiresOnTheIntervalItDeclares() {
    for (String name : List.of("Peashooter", "Repeater", "Snow Pea", "Mega Gatling Pea")) {
      int interval = declaredInterval(name);
      assertTrue(interval > 0, name + " declares no firing interval for the view to schedule to");

      List<Integer> fired = firingTicks(name, 200);
      assertTrue(fired.size() > 3, name + " barely fired: " + fired);
      for (int i = 1; i < fired.size(); i++) {
        assertEquals(interval, fired.get(i) - fired.get(i - 1),
            name + " fired " + (fired.get(i) - fired.get(i - 1)) + " ticks after the last shot but"
                + " declares " + interval + "; the wind-up animation would drift off the shot");
      }
    }
  }

  @Test
  void aLobberFiresOnTheIntervalItDeclares() {
    int interval = declaredInterval("Cabbage-pult");
    assertTrue(interval > 0, "Cabbage-pult declares no firing interval");
    List<Integer> fired = firingTicks("Cabbage-pult", 300);
    assertTrue(fired.size() > 2, "Cabbage-pult barely fired: " + fired);
    for (int i = 1; i < fired.size(); i++) {
      assertEquals(interval, fired.get(i) - fired.get(i - 1), "Cabbage-pult's cadence drifted");
    }
  }

  @Test
  void theFirstShotIsAlsoOnTheInterval() {
    // The view starts winding up before the very first shot too, counting from lastActionTick 0.
    for (String name : List.of("Peashooter", "Cabbage-pult")) {
      List<Integer> fired = firingTicks(name, 200);
      assertEquals(declaredInterval(name), (int) fired.get(0),
          name + " took its first shot off-schedule, so its first wind-up would not line up");
    }
  }

  @Test
  void aPlantWithNothingToShootAtNeverFires() {
    // The wind-up is gated on a target being in range; this is the state that gate stands for.
    Board board = new Board(5, 9);
    Plant plant = factory.createPlant("Peashooter", 2, 1);
    assertNotNull(plant);
    board.placePlant(plant);
    for (int tick = 1; tick <= 200; tick++) {
      board.updateAll(tick);
    }
    assertEquals(0, plant.getLastActionTick(),
        "an empty lane must leave the plant idle, or it would mime a shot it never takes");
  }

  @Test
  void everyShooterAndLobberInTheRosterDeclaresAnInterval() {
    List<String> silent = new ArrayList<>();
    for (var template : GameDataManager.plantRepository.getAll()) {
      String category = template.category == null ? "" : template.category.toLowerCase();
      if (!category.contains("shooter") && !category.contains("lobber")) {
        continue;
      }
      // The mints are filed under their family's category but do not shoot: they hand out plant
      // food and are built as MintAction, so there is no shot for a wind-up to lead.
      if (template.name != null && template.name.toLowerCase().endsWith("-mint")) {
        continue;
      }
      Plant plant = factory.createPlant(template.name, 2, 1);
      if (plant == null || plant.getBehavior() == null) {
        continue;
      }
      if (plant.getBehavior().actionIntervalTicks() <= 0) {
        silent.add(template.name);
      }
    }
    // Bowling Bulb runs three bulbs on three separate clocks, so it has no single interval.
    silent.remove("Bowling Bulb");
    assertTrue(silent.isEmpty(),
        "these shooters give the view no cadence to animate against: " + silent);
  }
}
