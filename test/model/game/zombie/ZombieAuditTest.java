package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.behavior.StandardZombieAction;
import model.game.zombie.behavior.TurquoiseZombieAction;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reproductions for the zombie defects found by walking the whole roster into a wall.
 *
 * <p>Each one is the scenario the sweep flagged, cut down to the smallest board that still shows
 * it: a zombie, one plant, and the thing that was going wrong.
 */
class ZombieAuditTest {

  private static PlantFactory plants;
  private static ZombieFactory zombieFactory;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    plants = new PlantFactory(GameDataManager.plantRepository);
    zombieFactory = new ZombieFactory(GameDataManager.zombieRepository);
  }

  /** A wall too tough to chew through in the time given, so "did it stop" is what is measured. */
  private static Plant unchewableWallAt(Board board, int row, int col) {
    Plant wall = plants.createPlant("Tall-nut", row, col);
    assertNotNull(wall);
    wall.grantBonusHealth(500_000);
    board.placePlant(wall);
    return wall;
  }

  private static double walkUntilStoppedOrHome(Board board, Zombie zombie) {
    for (int tick = 1; tick <= 900; tick++) {
      board.updateAll(tick);
      if (zombie.isDead() || zombie.getX() < -0.5) {
        break;
      }
    }
    return zombie.getX();
  }

  @Test
  void aCursedPlantStillBlocksTheLaneBehindIt() {
    Board board = new Board(5, 9);
    Plant wall = unchewableWallAt(board, 2, 3);

    Zombie hexer = new Zombie("Hex", 100, 0.0, 2, 8.5, new StandardZombieAction(20));
    board.spawnZombie(hexer);
    wall.applyCurse(hexer);

    Zombie walker = new Zombie("Walker", 500, 0.02, 2, 6.0, new StandardZombieAction(20));
    board.spawnZombie(walker);

    assertTrue(walkUntilStoppedOrHome(board, walker) > 2.6,
        "a sheep is still a plant standing on that tile; the lane must not open up");
  }

  @Test
  void aWizardHasToChewThroughAWallLikeAnythingElse() {
    Board board = new Board(5, 9);
    unchewableWallAt(board, 2, 3);
    Zombie wizard = zombieFactory.createZombie("ZombieWizardDefault", 2, 7.0);
    assertNotNull(wizard);
    board.spawnZombie(wizard);

    assertTrue(walkUntilStoppedOrHome(board, wizard) > 2.6,
        "the Wizard walked the whole lane; no barricade could stop it");
  }

  @Test
  void aWizardStillTurnsThePlantIntoASheep() {
    Board board = new Board(5, 9);
    Plant wall = unchewableWallAt(board, 2, 3);
    Zombie wizard = zombieFactory.createZombie("ZombieWizardDefault", 2, 4.0);
    assertNotNull(wizard);
    board.spawnZombie(wizard);

    walkUntilStoppedOrHome(board, wizard);
    assertTrue(wall.isCursed(), "the curse is the Wizard's whole point and must still happen");
  }

  @Test
  void aTurquoiseKillsThePlantItStoppedFor() {
    Board board = new Board(5, 9);
    // Exactly LASER_RANGE away: the case where detection reached and the laser did not.
    Plant target = plants.createPlant("Peashooter", 2, 3);
    assertNotNull(target);
    board.placePlant(target);
    Zombie turquoise = zombieFactory.createZombie("ZombieDarkTurquoiseDefault", 2, 7.0);
    assertNotNull(turquoise);
    board.spawnZombie(turquoise);

    for (int tick = 1; tick <= 300; tick++) {
      board.updateAll(tick);
    }
    assertTrue(target.isDead(),
        "it stopped four tiles away and fired past the plant for ever, never clearing the lane");
  }

  @Test
  void aTurquoiseMovesOnOnceTheLaneIsClear() {
    Board board = new Board(5, 9);
    plants.createPlant("Peashooter", 2, 3);
    Plant target = plants.createPlant("Peashooter", 2, 3);
    board.placePlant(target);
    Zombie turquoise = zombieFactory.createZombie("ZombieDarkTurquoiseDefault", 2, 7.0);
    assertNotNull(turquoise);
    double start = turquoise.getX();
    board.spawnZombie(turquoise);

    for (int tick = 1; tick <= 600; tick++) {
      board.updateAll(tick);
    }
    assertTrue(turquoise.getX() < start,
        "after vaporising the plant it should carry on, not stand there for the rest of the match");
  }

  @Test
  void aTacklerFlattensOnePlantPerCharge() {
    Board board = new Board(5, 9);
    Plant wall = unchewableWallAt(board, 2, 3);
    int before = wall.getCurrentHealth();
    Zombie allStar = zombieFactory.createZombie("ZombieModernAllStarDefault", 2, 6.0);
    assertNotNull(allStar);
    board.spawnZombie(allStar);

    walkUntilStoppedOrHome(board, allStar);
    int tackles = (before - wall.getCurrentHealth()) / 10_000;
    assertEquals(1, tackles,
        "the tackle repeated every tick it overlapped the plant instead of landing once");
  }

  @Test
  void everySeasonRosterIsMadeOfRealZombies() {
    for (model.environment.Season season : new model.environment.Season[] {
        new model.environment.AncientEgyptSeason(), new model.environment.FrostbiteCavesSeason(),
        new model.environment.BigWaveBeachSeason(), new model.environment.DarkAgesSeason()}) {
      for (Zombie zombie : season.getAvailableZombies()) {
        assertTrue(zombie.getMaxHealth() > 0,
            season.getName() + " can spawn " + zombie.getName() + ", which has no health");
        assertTrue(zombie.getSpeed() > 0,
            season.getName() + " can spawn " + zombie.getName() + ", which cannot move");
      }
    }
  }

  @Test
  void theTurquoiseLaserReachesAsFarAsItLooks() {
    // Guards the off-by-one directly, without depending on the zombie's timings.
    assertNotNull(new TurquoiseZombieAction(50, 10));
    Board board = new Board(5, 9);
    Plant near = plants.createPlant("Peashooter", 2, 6);
    Plant far = plants.createPlant("Peashooter", 2, 3);
    board.placePlant(near);
    board.placePlant(far);
    Zombie turquoise = zombieFactory.createZombie("ZombieDarkTurquoiseDefault", 2, 7.0);
    assertNotNull(turquoise);
    board.spawnZombie(turquoise);

    for (int tick = 1; tick <= 400; tick++) {
      board.updateAll(tick);
    }
    assertTrue(near.isDead() && far.isDead(),
        "both plants inside the laser's stated range should be gone");
  }
}
