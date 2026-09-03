package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.core.GameManager;
import model.game.Board;
import model.game.Projectile;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.behavior.StandardZombieAction;
import model.enums.ArmorType;
import model.enums.StatusEffect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reproductions for the issues raised in the visual/behavioural audit, kept as scenarios rather
 * than unit assertions: each one plants the plant the report names, runs the board, and checks the
 * thing the report says the player cannot see.
 */
class AuditReproductionTest {

  private static PlantFactory factory;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    factory = new PlantFactory(GameDataManager.plantRepository);
  }

  /** Plants one plant, parks a fat zombie in its lane and returns every shot the volley made. */
  private static List<Projectile> oneVolleyOf(String plantName) {
    Board board = new Board(5, 9);
    Plant plant = factory.createPlant(plantName, 2, 1);
    assertNotNull(plant, plantName + " did not build");
    board.placePlant(plant);
    board.spawnZombie(new Zombie("Target", 1_000_000, 0.0, 2, 7.0, new StandardZombieAction(20)));

    for (int tick = 1; tick <= 200; tick++) {
      board.updateAll(tick);
      List<Projectile> fired = new ArrayList<>(board.getProjectiles());
      if (!fired.isEmpty()) {
        return fired;
      }
    }
    return List.of();
  }

  @Test
  void repeaterFiresTwoPeas() {
    List<Projectile> volley = oneVolleyOf("Repeater");
    assertEquals(2, volley.size(),
        "Repeater's ability says 2 peas per volley; the player sees " + volley.size());
    assertEquals(20, volley.get(0).getDamage(), "each pea should carry the per-pea damage");
  }

  @Test
  void megaGatlingPeaFiresFourPeas() {
    List<Projectile> volley = oneVolleyOf("Mega Gatling Pea");
    assertEquals(4, volley.size(),
        "Mega Gatling Pea's ability says 4 peas per volley; the player sees " + volley.size());
    assertEquals(20, volley.get(0).getDamage(), "each pea should carry the per-pea damage");
  }

  @Test
  void threepeaterFiresOnePeaIntoEachOfThreeLanes() {
    List<Projectile> volley = oneVolleyOf("Threepeater");
    assertEquals(3, volley.size(), "Threepeater should put one pea in each of 3 lanes");
    List<Integer> rows = volley.stream().map(Projectile::getYCoordinate).sorted().toList();
    assertEquals(List.of(1, 2, 3), rows, "the three peas should be one per lane");
  }

  @Test
  void rotobagaFiresIntoFourDiagonals() {
    List<Projectile> volley = oneVolleyOf("Rotobaga");
    assertEquals(4, volley.size(),
        "Roto-baga's ability says 4 diagonal directions; found " + volley.size());
    assertEquals(10, volley.get(0).getDamage(),
        "10x3 is 10 a pea, not 30 -- Roto-baga was hitting for triple its listed damage");
  }

  @Test
  void aFivePeaPodFiresFivePeasAtDistinctHeights() {
    Board board = new Board(5, 9);
    Plant pod = factory.createPlant("Pea Pod", 2, 1);
    assertNotNull(pod);
    board.placePlant(pod);
    for (int extra = 0; extra < 4; extra++) {
      assertTrue(pod.addStack(), "Pea Pod should stack up to five heads");
    }
    assertEquals(5, pod.getStackCount());
    board.spawnZombie(new Zombie("Target", 1_000_000, 0.0, 2, 7.0, new StandardZombieAction(20)));

    List<Projectile> volley = List.of();
    for (int tick = 1; tick <= 200 && volley.isEmpty(); tick++) {
      board.updateAll(tick);
      volley = new ArrayList<>(board.getProjectiles());
    }
    assertEquals(5, volley.size(), "a five-head Pea Pod should fire five peas");
    long distinctHeights = volley.stream().map(Projectile::getMuzzleOffset).distinct().count();
    assertEquals(5, distinctHeights,
        "the five peas overlap exactly, so the player only ever sees one");
  }

  @Test
  void aSpentCactusSpikeLeavesTheLawn() {
    Board board = new Board(5, 9);
    Plant cactus = factory.createPlant("Cactus", 2, 0);
    assertNotNull(cactus);
    board.placePlant(cactus);
    for (int i = 0; i < 4; i++) {
      board.spawnZombie(
          new Zombie("Crowd" + i, 1_000_000, 0.0, 2, 3.0 + i, new StandardZombieAction(20)));
    }

    for (int tick = 1; tick <= 600; tick++) {
      board.updateAll(tick);
      assertTrue(board.getProjectiles().size() < 40,
          "spikes are piling up on the lawn: " + board.getProjectiles().size() + " at tick " + tick);
    }
    long stuck = board.getProjectiles().stream().filter(p -> !p.isActive()).count();
    assertEquals(0, stuck, "spent spikes are still sitting on the lawn");
  }

  @Test
  void graveBusterCanBePlantedOnAGrave() {
    GameManager match = newMatch();
    match.getBoard().getTile(2, 3).setEffect(new TombStoneEffect(700, true));
    Plant buster = factory.createPlant("Grave Buster", 2, 3);
    assertNotNull(buster);
    assertTrue(match.placePlant(buster, 2, 3),
        "Grave Buster exists to be planted on a gravestone and cannot be");
  }

  @Test
  void graveBusterStillCannotBePlantedOnAPlainTile() {
    GameManager match = newMatch();
    Plant buster = factory.createPlant("Grave Buster", 2, 3);
    assertNotNull(buster);
    assertTrue(match.placePlant(buster, 2, 3), "an empty tile was always plantable");
  }

  @Test
  void peashooterStillCannotBePlantedOnAGrave() {
    GameManager match = newMatch();
    match.getBoard().getTile(2, 3).setEffect(new TombStoneEffect(700, true));
    Plant peashooter = factory.createPlant("Peashooter", 2, 3);
    assertNotNull(peashooter);
    assertFalse(match.placePlant(peashooter, 2, 3), "a grave still blocks an ordinary plant");
  }

  @Test
  void hotPotatoCanBePlantedOnAnIceTrail() {
    GameManager match = newMatch();
    match.getBoard().getTile(2, 3).setEffect(new IceTrailEffect(600, 0.5));
    Plant potato = factory.createPlant("Hot Potato", 2, 3);
    assertNotNull(potato);
    assertTrue(match.placePlant(potato, 2, 3),
        "Hot Potato exists to be planted on ice and cannot be");

    for (int tick = 1; tick <= 40; tick++) {
      match.getBoard().updateAll(tick);
    }
    assertTrue(match.getBoard().getTile(2, 3).getEffect() == null
            || !match.getBoard().getTile(2, 3).getEffect().isActive(),
        "the ice was never melted");
  }

  @Test
  void peashooterStillCannotBePlantedOnAnIceTrail() {
    GameManager match = newMatch();
    match.getBoard().getTile(2, 3).setEffect(new IceTrailEffect(600, 0.5));
    Plant peashooter = factory.createPlant("Peashooter", 2, 3);
    assertNotNull(peashooter);
    assertFalse(match.placePlant(peashooter, 2, 3), "slippery ice still blocks an ordinary plant");
  }

  @Test
  void magnetShroomTakesBothPiecesOfMetalInOnePull() {
    Zombie knight = armouredKnightAt(2, 3.0);
    runMagnetFor(knight, 110);
    List<Armor> left =
        knight.getArmors().stream().filter(a -> a.isMetallic() && !a.isDestroyed()).toList();
    assertTrue(left.isEmpty(),
        "one pull (10s cycle) should take every piece of metal; " + left.size() + " left on the zombie");
  }

  @Test
  void magnetShroomReachesTheLaneNextToIt() {
    Zombie knight = armouredKnightAt(1, 3.0);
    runMagnetFor(knight, 300);
    List<Armor> left =
        knight.getArmors().stream().filter(a -> a.isMetallic() && !a.isDestroyed()).toList();
    assertTrue(left.isEmpty(), "a magnet that only reaches its own lane is not a magnet");
  }

  @Test
  void magnetShroomDoesNotReachAcrossTheWholeLawn() {
    Zombie knight = armouredKnightAt(2, 8.0);
    runMagnetFor(knight, 300);
    assertTrue(knight.getArmors().stream().anyMatch(a -> a.isMetallic() && !a.isDestroyed()),
        "the magnet has a range and a zombie eight tiles away is outside it");
  }

  private static Zombie armouredKnightAt(int row, double x) {
    Zombie knight = new Zombie("Knight", 200, 0.0, row, x, new StandardZombieAction(20));
    knight.addArmor(new Armor("Helmet", 1100, true, ArmorType.HELMET));
    knight.addArmor(new Armor("Bucket", 370, true, ArmorType.BUCKET));
    return knight;
  }

  private static void runMagnetFor(Zombie zombie, int ticks) {
    Board board = new Board(5, 9);
    Plant magnet = factory.createPlant("Magnet-shroom", 2, 1);
    assertNotNull(magnet);
    board.placePlant(magnet);
    board.spawnZombie(zombie);
    for (int tick = 1; tick <= ticks; tick++) {
      board.updateAll(tick);
    }
  }

  @Test
  void butterStunsWithoutFreezing() {
    Zombie zombie = new Zombie("Target", 500, 0.02, 2, 5.0, new StandardZombieAction(20));
    Projectile butter =
        new Projectile(20, 0.5, 1, 2, Projectile.ProjectileEffect.NORMAL, false, true, false);
    butter.firedBy("Kernel-pult").withStun(30);
    butter.hitZombie(zombie);

    assertTrue(zombie.getActiveEffects().containsKey(StatusEffect.STUNNED), "butter should stun");
    assertFalse(zombie.getActiveEffects().containsKey(StatusEffect.FROZEN),
        "a stun drawn as a freeze tells the player the wrong thing");
    double before = zombie.getX();
    zombie.move();
    assertEquals(before, zombie.getX(), 1e-9, "a stunned zombie still has to stand still");
  }

  @Test
  void wasabiWhipThawsTheIceBesideIt() {
    Board board = new Board(5, 9);
    Plant wasabi = factory.createPlant("Wasabi Whip", 2, 3);
    assertNotNull(wasabi);
    board.placePlant(wasabi);
    board.getTile(2, 4).setEffect(new IceTrailEffect(600, 0.5));
    board.getTile(1, 3).setEffect(new IceTrailEffect(600, 0.5));

    for (int tick = 1; tick <= 60; tick++) {
      board.updateAll(tick);
    }
    assertTrue(isThawed(board, 2, 4), "the ice in front of it is still there");
    assertTrue(isThawed(board, 1, 3), "the ice in the lane beside it is still there");
  }

  @Test
  void wasabiWhipDoesNotThawTheWholeLawn() {
    Board board = new Board(5, 9);
    Plant wasabi = factory.createPlant("Wasabi Whip", 2, 0);
    assertNotNull(wasabi);
    board.placePlant(wasabi);
    board.getTile(2, 8).setEffect(new IceTrailEffect(600, 0.5));

    for (int tick = 1; tick <= 60; tick++) {
      board.updateAll(tick);
    }
    assertFalse(isThawed(board, 2, 8), "its heat should reach one tile, not the far wall");
  }

  @Test
  void gooKeepsDamagingAZombieAfterTheHit() {
    Zombie zombie = new Zombie("Target", 500, 0.0, 2, 5.0, new StandardZombieAction(20));
    Projectile goo =
        new Projectile(20, 0.5, 1, 2, Projectile.ProjectileEffect.POISON, false, false, false);
    goo.firedBy("Goo Peashooter");
    goo.hitZombie(zombie);

    assertTrue(zombie.getActiveEffects().containsKey(StatusEffect.POISONED),
        "the goo left no lasting effect, so \"damage over time\" never happened");
    int afterHit = zombie.getCurrentHealth();
    for (int tick = 1; tick <= 10; tick++) {
      zombie.update(tick, new Board(5, 9));
    }
    assertTrue(zombie.getCurrentHealth() < afterHit,
        "a poisoned zombie should still be losing health after the shot landed");
  }

  @Test
  void gooIgnoresArmour() {
    Zombie zombie = new Zombie("Bucket", 500, 0.0, 2, 5.0, new StandardZombieAction(20));
    zombie.addArmor(new Armor("Bucket", 1100, true, ArmorType.BUCKET));
    int before = zombie.getCurrentHealth();
    Projectile goo =
        new Projectile(20, 0.5, 1, 2, Projectile.ProjectileEffect.POISON, false, false, false);
    goo.hitZombie(zombie);
    assertTrue(zombie.getCurrentHealth() < before, "goo is documented to ignore armour");
  }

  private static boolean isThawed(Board board, int row, int col) {
    return board.getTile(row, col).getEffect() == null
        || !board.getTile(row, col).getEffect().isActive();
  }

  private static GameManager newMatch() {
    GameManager match = new GameManager(new Board(5, 9));
    match.startGame();
    match.enableFreePlanting();
    return match;
  }
}
