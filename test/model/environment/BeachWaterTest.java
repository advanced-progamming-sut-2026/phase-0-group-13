package model.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.enums.ZombieType;
import model.game.Board;
import model.game.TileEffects.IceTrailEffect;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Big Wave Beach's water, and who is allowed to be standing in it.
 *
 * <p>Water only ever gated where you could plant. Nothing looked at it for zombies, and
 * {@link Season#rosterOf} puts the fifteen common land zombies in front of whatever a season asks
 * for -- so the beach was spawning Coneheads and All-Stars into open sea at the right-hand column
 * and walking them across it.
 */
class BeachWaterTest {

  private static ZombieFactory zombies;
  private static PlantFactory plants;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    zombies = new ZombieFactory(GameDataManager.zombieRepository);
    plants = new PlantFactory(GameDataManager.plantRepository);
  }

  @Test
  void everyZombieTheBeachCanSpawnIsAbleToBeInWater() {
    BigWaveBeachSeason beach = new BigWaveBeachSeason();
    List<String> landlocked = new ArrayList<>();
    for (Zombie zombie : beach.getAvailableZombies()) {
      if (!zombie.canCrossWater()) {
        landlocked.add(zombie.getName());
      }
    }
    assertTrue(landlocked.isEmpty(),
        "the beach would spawn these into the sea: " + landlocked);
  }

  @Test
  void theBeachStillHasAZombiePool() {
    assertFalse(new BigWaveBeachSeason().getAvailableZombies().isEmpty(),
        "filtering the roster must not empty it, or the chapter has no zombies at all");
  }

  @Test
  void theDryChaptersAreUnaffected() {
    for (Season season : new Season[] {new AncientEgyptSeason(), new DarkAgesSeason(),
        new FrostbiteCavesSeason()}) {
      assertTrue(season.getAvailableZombies().size() > 10,
          season.getName() + " lost its land zombies; only the beach should be filtered");
    }
  }

  @Test
  void onlyTheSeaRosterAndTheBossesCanCrossWater() {
    List<ZombieType> swimmers = new ArrayList<>();
    for (ZombieType type : ZombieType.values()) {
      if (type.canCrossWater()) {
        swimmers.add(type);
      }
    }
    assertEquals(List.of(ZombieType.FISHERMAN, ZombieType.SNORKEL, ZombieType.OCTOPUS,
            ZombieType.ZOMBOSS_EGYPT, ZombieType.ZOMBOSS_PIRATE, ZombieType.ZOMBOSS_COWBOY,
            ZombieType.ZOMBOSS_DARK),
        swimmers, "the set of water-capable zombies changed");
  }

  @Test
  void theBoardKnowsWhenWaterIsImpassable() {
    Board board = new Board(5, 9);
    board.setWaterAt(2, 7, true);

    Zombie lander = zombies.createZombie("ZombieMummyDefault", 2, 7.0);
    Zombie swimmer = zombies.createZombie("ZombieBeachSnorkel", 2, 7.0);
    assertNotNull(lander);
    assertNotNull(swimmer);

    assertTrue(board.isImpassableWaterFor(lander, 2, 7.0),
        "a land zombie must not be standing in the sea");
    assertFalse(board.isImpassableWaterFor(swimmer, 2, 7.0), "the Snorkel swims");
    assertFalse(board.isImpassableWaterFor(lander, 2, 3.0), "dry land is fine for anyone");
  }

  @Test
  void theTideStillFloodsTheRightHandColumns() {
    BigWaveBeachSeason beach = new BigWaveBeachSeason();
    Board board = new Board(5, 9);
    beach.placeHazards(board);
    assertTrue(board.isWaterAt(2, 8), "the far column is sea at any tide");
    assertFalse(board.isWaterAt(2, 0), "the house end is dry");
  }

  @Test
  void aBurningShotClearsTheIceTrailItFliesOver() {
    // Torchwood's doc says a pea passing through it melts ice; only plant-ice was ever handled.
    Board board = new Board(5, 9);
    Plant firePea = plants.createPlant("Fire Peashooter", 2, 2);
    assertNotNull(firePea);
    board.placePlant(firePea);
    board.getTile(2, 4).setEffect(new IceTrailEffect(600, 0.5));
    board.spawnZombie(new Zombie("Target", 1_000_000, 0.0, 2, 7.0, new StandardZombieAction(20)));

    for (int tick = 1; tick <= 120; tick++) {
      board.updateAll(tick);
    }
    assertTrue(board.getTile(2, 4).getEffect() == null
            || !board.getTile(2, 4).getEffect().isActive(),
        "the frozen ground of the caves ignored a shot that was on fire");
  }

  @Test
  void anOrdinaryShotLeavesTheIceTrailAlone() {
    Board board = new Board(5, 9);
    Plant peashooter = plants.createPlant("Peashooter", 2, 2);
    assertNotNull(peashooter);
    board.placePlant(peashooter);
    board.getTile(2, 4).setEffect(new IceTrailEffect(600, 0.5));
    board.spawnZombie(new Zombie("Target", 1_000_000, 0.0, 2, 7.0, new StandardZombieAction(20)));

    for (int tick = 1; tick <= 120; tick++) {
      board.updateAll(tick);
    }
    assertNotNull(board.getTile(2, 4).getEffect(), "only fire melts ice");
    assertTrue(board.getTile(2, 4).getEffect().isActive(), "a plain pea must not thaw the ground");
  }
}
