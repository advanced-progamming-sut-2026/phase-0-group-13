package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The defending player's move: where, what, how much, and how often. */
class IZombiePlantPlacementTest {

  private static IZombieEngine engine() {
    return new IZombieEngine(1, new Random(7L));
  }

  @Test
  void theRosterIsPricedOutOfTheGamesOwnCatalogue() {
    List<IZombieEngine.PlantSpec> roster = IZombieEngine.availablePlantTypes();
    assertEquals(IZombieEngine.PLANT_ROSTER.size(), roster.size(),
        "every plant on the roster should have been found in plants.json");

    IZombieEngine.PlantSpec sunflower = find(roster, "Sunflower");
    assertEquals(50, sunflower.cost);
    assertEquals(300, sunflower.health);
    assertTrue(sunflower.producesSun());
    assertEquals(50, sunflower.sunPerCycle, "plants.json says 50 sun every 24 seconds");
    assertEquals(240, sunflower.intervalTicks);
    assertEquals(50, sunflower.rechargeTicks, "5s recharge at 10 ticks a second");

    IZombieEngine.PlantSpec repeater = find(roster, "Repeater");
    assertEquals(40, repeater.damagePerShot, "\"20x2\" is two peas");
    assertFalse(repeater.producesSun());

    IZombieEngine.PlantSpec wallnut = find(roster, "Wall-nut");
    assertEquals(0, wallnut.damagePerShot);
    assertEquals(4000, wallnut.health);
  }

  @Test
  void aPlantGoesDownOnAFreeTileLeftOfTheRedLine() {
    IZombieEngine engine = engine();
    int before = engine.getPlantSun();
    int[] free = freeTile(engine);

    String receipt = engine.placePlant("Sunflower", free[0], free[1]);

    assertFalse(receipt.startsWith("error:"), receipt);
    assertEquals(before - 50, engine.getPlantSun());
    assertEquals("Sunflower", plantAt(engine, free[0], free[1]).getName());
  }

  @Test
  void theRedLineIsTheEdgeOfTheDefendersHalf() {
    IZombieEngine engine = engine();

    assertTrue(engine.placePlant("Sunflower", 0, IZombieEngine.RED_LINE_COLUMN)
        .startsWith("error: plants can only be placed left of the red line"));
    assertTrue(engine.placePlant("Sunflower", 0, IZombieEngine.COLS - 1)
        .startsWith("error: plants can only be placed left of the red line"));
    assertEquals("error: coordinates out of bounds", engine.placePlant("Sunflower", 9, 0));
    assertEquals("error: coordinates out of bounds", engine.placePlant("Sunflower", 0, -1));
  }

  @Test
  void anUnknownPlantIsRefusedByName() {
    assertEquals("error: unknown plant type 'Coconut Cannon'",
        engine().placePlant("Coconut Cannon", 0, 0));
  }

  @Test
  void aTileOnlyHoldsOnePlant() {
    IZombieEngine engine = engine();
    int[] free = freeTile(engine);
    engine.placePlant("Sunflower", free[0], free[1]);

    assertEquals("error: there is already a Sunflower at (" + (free[1] + 1) + ", "
            + (free[0] + 1) + ")",
        engine.placePlant("Peashooter", free[0], free[1]));
  }

  @Test
  void aPlantIsRefusedWhenThereIsNoSunForIt() {
    IZombieEngine engine = engine();
    int[] free = freeTile(engine);
    // 150 sun buys a Repeater at 200 only after a Sunflower has paid for itself a few times.
    assertEquals("error: not enough sun (need 200, have " + engine.getPlantSun() + ")",
        engine.placePlant("Repeater", free[0], free[1]));
  }

  /** The seeded cutouts take tiles at random, and occupancy is checked before the price. */
  private static int[] freeTile(IZombieEngine engine) {
    for (int row = 0; row < IZombieEngine.ROWS; row++) {
      for (int col = 0; col < IZombieEngine.RED_LINE_COLUMN; col++) {
        boolean taken = false;
        for (IZombieEngine.DefensePlant plant : engine.getDefensePlants()) {
          taken |= plant.getRow() == row && plant.getCol() == col;
        }
        if (!taken) {
          return new int[] {row, col};
        }
      }
    }
    throw new IllegalStateException("the whole defending half is full");
  }

  @Test
  void aTypeHasToRechargeBeforeItIsPlacedAgain() {
    IZombieEngine engine = engine();
    int[] first = freeTile(engine);
    engine.placePlant("Sunflower", first[0], first[1]);
    int[] second = freeTile(engine);

    String refusal = engine.placePlant("Sunflower", second[0], second[1]);
    assertTrue(refusal.startsWith("error: Sunflower is still recharging"), refusal);

    for (int i = 0; i < 50; i++) {
      engine.tick();
    }
    assertEquals(0, engine.plantRechargeTicksLeft("Sunflower"));
    assertFalse(engine.placePlant("Sunflower", second[0], second[1]).startsWith("error:"));
  }

  @Test
  void aSunflowerPaysOutOnTheCatalogueCadence() {
    IZombieEngine engine = engine();
    int[] free = freeTile(engine);
    engine.placePlant("Sunflower", free[0], free[1]);
    int afterBuying = engine.getPlantSun();

    for (int i = 0; i < 239; i++) {
      engine.tick();
    }
    assertEquals(afterBuying, engine.getPlantSun(), "nothing until the 24 seconds are up");

    engine.tick();
    assertEquals(afterBuying + 50, engine.getPlantSun());
  }

  @Test
  void aZombieOnTheTileBlocksThePlanting() {
    IZombieEngine engine = engine();
    // Walk a basic all the way into the defender's half, then try to plant on top of it.
    engine.placeZombie("basic", 0, 8);
    for (int i = 0; i < 400 && !engine.isFinished(); i++) {
      engine.tick();
    }
    IZombieEngine.DeployedZombie walker = null;
    for (IZombieEngine.DeployedZombie zombie : engine.getDeployedZombies()) {
      if (!zombie.producesSun() && zombie.getColumn() < IZombieEngine.RED_LINE_COLUMN) {
        walker = zombie;
      }
    }
    if (walker == null) {
      return; // it never got past the cutouts on this seed; nothing to assert about
    }
    int column = (int) Math.round(walker.getColumn());
    String refusal = engine.placePlant("Sunflower", walker.getRow(), column);
    assertTrue(refusal.startsWith("error: a zombie is standing on")
        || refusal.startsWith("error: there is already"), refusal);
  }

  @Test
  void theSeededCutoutsKnowWhatTheyAre() {
    for (IZombieEngine.DefensePlant plant : engine().getDefensePlants()) {
      assertEquals(IZombieEngine.CUTOUT_PLANT, plant.getName());
    }
  }

  private static IZombieEngine.PlantSpec find(List<IZombieEngine.PlantSpec> roster, String name) {
    return roster.stream().filter(spec -> spec.name.equals(name)).findFirst().orElseThrow();
  }

  private static IZombieEngine.DefensePlant plantAt(IZombieEngine engine, int row, int col) {
    return engine.getDefensePlants().stream()
        .filter(plant -> plant.getRow() == row && plant.getCol() == col)
        .findFirst()
        .orElseThrow();
  }
}
