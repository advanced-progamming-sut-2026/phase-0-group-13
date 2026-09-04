package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.game.minigame.arcade.IZombieEngine.DefensePlant;
import model.game.minigame.arcade.IZombieEngine.DeployedZombie;
import model.game.minigame.arcade.IZombieEngine.Shot;
import org.junit.jupiter.api.Test;

/**
 * The peas the lawn's cutouts fire at the zombie player.
 *
 * <p>They used to take their damage off the nearest zombie in range on the tick they fired, with
 * nothing between the two: health came off a zombie several tiles from a plant that never appeared
 * to do anything, and in a network match -- where the screen only ever draws what the snapshot
 * carries -- there was no shooting on screen at all.
 */
class IZombieShotTest {

  private static final String WALKER = "basic";
  /** The reach the cutouts are seeded with, and so how far one of their peas may fly. */
  private static final double CUTOUT_RANGE = 5.0;

  private static IZombieEngine engine() {
    return new IZombieEngine(1, new Random(7L));
  }

  /** A lane with one cutout in it, and the cutout, so a test can watch a single exchange. */
  private static DefensePlant loneCutoutRow(IZombieEngine engine) {
    for (DefensePlant plant : engine.getDefensePlants()) {
      if (plant.getCol() <= 2 && !plant.producesSun()) {
        return plant;
      }
    }
    return null;
  }

  private static DeployedZombie walkerIn(IZombieEngine engine, int row) {
    for (DeployedZombie zombie : engine.getDeployedZombies()) {
      if (zombie.getRow() == row && !zombie.producesSun()) {
        return zombie;
      }
    }
    return null;
  }

  @Test
  void aCutoutPutsARealPeaOnTheLawnWhenItFires() {
    IZombieEngine engine = engine();
    DefensePlant cutout = loneCutoutRow(engine);
    assertNotNull(cutout, "the level should seed a cutout near the house end");
    assertTrue(engine.placeZombie(WALKER, cutout.getRow(), IZombieEngine.COLS - 2)
        .startsWith("Deployed"), "the walker should have gone down");

    for (int tick = 0; tick < IZombieEngine.PLANT_FIRE_INTERVAL * 3 && engine.getShots().isEmpty();
        tick++) {
      engine.tick();
    }
    List<Shot> shots = engine.getShots();
    assertFalse(shots.isEmpty(), "the cutout fired and nothing was put on the lawn to show it");
    for (Shot pea : shots) {
      assertTrue(pea.getRow() >= 0 && pea.getRow() < IZombieEngine.ROWS,
          "a pea turned up outside the lawn");
      assertTrue(pea.getColumn() > 0, "a pea should have left the plant that fired it");
    }
  }

  @Test
  void theDamageLandsWhenThePeaArrivesAndNotBefore() {
    IZombieEngine engine = engine();
    DefensePlant cutout = loneCutoutRow(engine);
    assertNotNull(cutout);
    engine.placeZombie(WALKER, cutout.getRow(), IZombieEngine.COLS - 2);
    DeployedZombie walker = walkerIn(engine, cutout.getRow());
    assertNotNull(walker);

    int health = walker.getHealth();
    boolean sawAPeaInFlight = false;
    for (int tick = 0; tick < IZombieEngine.PLANT_FIRE_INTERVAL * 4; tick++) {
      engine.tick();
      boolean inFlight = !engine.getShots().isEmpty();
      if (walker.getHealth() < health) {
        assertTrue(sawAPeaInFlight,
            "the zombie lost health on a tick no pea had ever been drawn crossing the lawn");
        return;
      }
      sawAPeaInFlight |= inFlight;
    }
    throw new AssertionError("the cutout never actually hurt the zombie in front of it");
  }

  @Test
  void aPeaReachesNoFurtherThanTheInstantHitDid() {
    // The instant hit only ever took health off a zombie within a cutout's five tiles. Giving the
    // shot a flight must not quietly extend that: a pea that outlived its target used to be able
    // to carry on into the sun-imps parked at the far column.
    IZombieEngine engine = engine();
    Map<DeployedZombie, Integer> health = new HashMap<>();
    for (DeployedZombie zombie : engine.getDeployedZombies()) {
      health.put(zombie, zombie.getHealth());
    }
    for (int tick = 0; tick < IZombieEngine.PLANT_FIRE_INTERVAL * 8; tick++) {
      engine.tick();
      for (DeployedZombie zombie : engine.getDeployedZombies()) {
        Integer was = health.get(zombie);
        if (was == null || zombie.getHealth() >= was) {
          continue;
        }
        health.put(zombie, zombie.getHealth());
        assertTrue(hasCutoutWithinReach(engine, zombie),
            "something shot a zombie at column " + zombie.getColumn() + " in row "
                + zombie.getRow() + " from further than a cutout can reach");
      }
    }
  }

  private static boolean hasCutoutWithinReach(IZombieEngine engine, DeployedZombie zombie) {
    for (DefensePlant plant : engine.getDefensePlants()) {
      double distance = zombie.getColumn() - plant.getCol();
      if (plant.getRow() == zombie.getRow() && distance >= 0 && distance <= CUTOUT_RANGE) {
        return true;
      }
    }
    return false;
  }

  @Test
  void theCutoutsCanStillKillWhatWalksIntoThem() {
    IZombieEngine engine = engine();
    DefensePlant cutout = loneCutoutRow(engine);
    assertNotNull(cutout);
    engine.placeZombie(WALKER, cutout.getRow(), IZombieEngine.COLS - 2);
    DeployedZombie walker = walkerIn(engine, cutout.getRow());
    assertNotNull(walker);

    for (int tick = 0; tick < 600 && !walker.isDead(); tick++) {
      engine.tick();
    }
    assertTrue(walker.isDead(),
        "giving the peas a flight must not stop the lawn defending itself");
  }

  @Test
  void theSnapshotCarriesThePeasAndTheFiringCadence() {
    IZombieMatch match = new IZombieMatch(1, 7L);
    IZombieEngine engine = match.getEngine();
    DefensePlant cutout = loneCutoutRow(engine);
    assertNotNull(cutout);
    engine.placeZombie(WALKER, cutout.getRow(), IZombieEngine.COLS - 2);

    boolean sawAPea = false;
    for (int tick = 0; tick < IZombieEngine.PLANT_FIRE_INTERVAL * 3; tick++) {
      match.tick();
      IZombieMatch.Snapshot state = match.snapshot();
      assertNotNull(state.shots(), "a snapshot with no shots list draws no shooting at all");
      sawAPea |= !state.shots().isEmpty();
      for (IZombieMatch.PlantView plant : state.plants()) {
        assertTrue(plant.ticksToShot() <= IZombieEngine.PLANT_FIRE_INTERVAL,
            "a cutout cannot be further from its next shot than its own interval");
      }
    }
    assertTrue(sawAPea, "the screens only draw what the snapshot carries, and it carried no pea");
  }
}
