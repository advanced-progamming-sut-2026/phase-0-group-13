package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.enums.SunType;
import model.game.Board;
import model.game.Projectile;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The behaviours a zombie-by-zombie audit found broken against the doc. Each test is the
 * reproduction that failed before the fix, so the doc's rule cannot quietly regress again.
 */
class ZombieSpecAuditTest {

  /** What a plant actually fires at, so the Jester is tested against a real shot speed. */
  private static final double PEA_SPEED = 0.5;

  private static ZombieFactory zombies;
  private static PlantFactory plants;

  @BeforeAll
  static void loadData() {
    new GameDataManager();
    zombies = new ZombieFactory(GameDataManager.zombieRepository);
    plants = new PlantFactory(GameDataManager.plantRepository);
  }

  private static Board board() {
    Board board = new Board(5, 9);
    board.initialize();
    board.getGameState().setSkySunDisabled(true);
    return board;
  }

  private static Zombie spawn(Board board, String alias, int row, double x) {
    Zombie zombie = zombies.createZombie(alias, row, x);
    board.spawnZombie(zombie);
    return zombie;
  }

  private static Plant plant(Board board, String name, int row, int col) {
    Plant p = plants.createPlant(name, row, col);
    board.placePlant(p);
    return p;
  }

  private static void run(Board board, int ticks) {
    for (int tick = 0; tick < ticks; tick++) {
      board.updateAll(tick);
    }
  }

  /** "During spinning, all straight shots that come at it are returned towards the plants." */
  @Test
  void theJesterCatchesAShotTravellingAtPlantSpeed() {
    Board board = board();
    Zombie jester = spawn(board, "ZombieDarkJugglerDefault", 2, 5.0);
    int before = jester.getCurrentHealth();
    board.addProjectile(new Projectile(50, PEA_SPEED, 1.0, 2,
        Projectile.ProjectileEffect.NORMAL, false, false, false));

    run(board, 40);

    assertEquals(before, jester.getCurrentHealth(),
        "a straight shot at plant speed should be juggled away, not land");
  }

  /** "After eating a plant, it continues at a very slow pace." */
  @Test
  void theAllStarSlowsDownOnceItHasFlattenedAPlant() {
    Board board = board();
    Plant wall = plant(board, "Wallnut", 2, 4);
    Zombie allStar = spawn(board, "ZombieModernAllStarDefault", 2, 6.0);

    double sprintStart = allStar.getX();
    run(board, 100);
    double sprintRate = (sprintStart - allStar.getX()) / 100.0;

    run(board, 100);
    assertTrue(wall.isDead(), "it should have run the wall-nut down by now");
    double crawlStart = allStar.getX();
    run(board, 100);
    double crawlRate = (crawlStart - allStar.getX()) / 100.0;

    assertTrue(crawlRate < sprintRate,
        "the All-Star should crawl after its first kill, was " + sprintRate + " now " + crawlRate);
  }

  /** "Like the rest of the Imps, except that fire shots have no effect on it." */
  @Test
  void theImpDragonIgnoresFire() {
    Board board = board();
    Zombie dragon = spawn(board, "ZombieDarkImpDragonDefault", 2, 6.0);
    int before = dragon.getCurrentHealth();
    board.addProjectile(new Projectile(50, PEA_SPEED, 4.0, 2,
        Projectile.ProjectileEffect.FIRE, false, false, false));

    run(board, 40);

    assertEquals(before, dragon.getCurrentHealth(), "fire should not touch the Imp Dragon");
  }

  @Test
  void theImpDragonEatsAPlantInsteadOfFlatteningIt() {
    Board board = board();
    Plant wall = plant(board, "Wallnut", 2, 3);
    spawn(board, "ZombieDarkImpDragonDefault", 2, 3.2);

    run(board, 30);

    assertFalse(wall.isDead(), "an imp chews a wall-nut down, it does not one-shot it");
    assertTrue(wall.getCurrentHealth() < wall.getMaxHealth(), "it should still be biting");
  }

  /** "Flies over obstacles such as the wall-nut", but ordinary plants are food. */
  @Test
  void theDodoFliesOverAWallNutButEatsAPeashooter() {
    Board overWall = board();
    Plant wall = plant(overWall, "Wallnut", 2, 3);
    Zombie dodo = spawn(overWall, "ZombieIceAgeDodo", 2, 3.4);
    run(overWall, 200);
    assertEquals(wall.getMaxHealth(), wall.getCurrentHealth(), "a wall-nut is flown over, not eaten");
    assertTrue(dodo.getX() < 2.0, "the dodo should have carried on past it");

    Board overPea = board();
    Plant pea = plant(overPea, "Peashooter", 2, 3);
    spawn(overPea, "ZombieIceAgeDodo", 2, 3.4);
    run(overPea, 300);
    assertTrue(pea.isDead(), "an ordinary plant is not an obstacle, it is lunch");
  }

  /** "After the explosion the dynamite is flung to the end of the row and rolls back." */
  @Test
  void theProspectorsDynamiteRollsTheRowAndClearsIt() {
    Board board = board();
    Plant near = plant(board, "Wallnut", 2, 1);
    Plant far = plant(board, "Wallnut", 2, 4);
    Zombie prospector = spawn(board, "ZombieProspectorDefault", 2, 7.5);

    run(board, 300);

    assertFalse(prospector.isDead(), "the thrower should not be killed by its own dynamite");
    assertTrue(near.isDead() && far.isDead(),
        "the rolling dynamite should have taken both plants in the row");
  }

  /** "It drags the sun on the ground to itself; when killed the stolen sun goes back." */
  @Test
  void theRaZombieStealsGroundSunAndGivesItBackWhenKilled() {
    Board board = board();
    Zombie ra = spawn(board, "ZombieRaDefault", 2, 5.0);
    for (int i = 0; i < 4; i++) {
      Sun sun = new Sun(25, 600, SunType.NORMAL, false);
      sun.changinCordinate(4.6 + i * 0.1, 2.0);
      board.addSun(sun);
    }
    int sunsBefore = board.getSuns().size();

    run(board, 200);

    assertTrue(board.getSuns().size() < sunsBefore,
        "Ra should have pulled sun off the lawn, still " + board.getSuns().size());
    int playerSun = board.getGameState().getCurrentSun();
    ra.takeDamage(ra.getMaxHealth() * 5, true);
    board.updateAll(300);
    assertTrue(board.getGameState().getCurrentSun() > playerSun,
        "killing the thief should hand the stolen sun back");
  }

  /** "Stays in the rightmost column and does not move; knights a simple zombie beside it." */
  @Test
  void theKingHoldsHisColumnAndKnightsHisNeighbour() {
    Board board = board();
    Zombie king = spawn(board, "ZombieDarkKing", 2, 8.0);
    Zombie subject = spawn(board, "ZombieMummyDefault", 2, 7.5);
    double kingX = king.getX();

    run(board, 300);

    assertEquals(kingX, king.getX(), 1e-9, "the King does not walk");
    assertEquals(2, subject.getArmors().size(),
        "a neighbour should have been given a helmet and shoulder armour");
  }

  /** "On reaching water it moves below it" -- on dry land it walks and eats like anything else. */
  @Test
  void theSnorkelEatsOnDryLandAndOnlyDivesInWater() {
    Board dry = board();
    Plant pea = plant(dry, "Peashooter", 2, 3);
    Zombie diver = spawn(dry, "ZombieBeachSnorkel", 2, 3.4);
    run(dry, 300);
    assertTrue(pea.isDead(), "on dry ground the diver has to eat what is in front of it");
    assertFalse(diver.isSubmerged(), "there is no water here to dive into");

    Board sea = board();
    sea.setWaterAt(2, 6, true);
    Zombie swimmer = spawn(sea, "ZombieBeachSnorkel", 2, 6.0);
    sea.updateAll(0);
    assertTrue(swimmer.isSubmerged(), "over water it should be under the surface");
  }
}
