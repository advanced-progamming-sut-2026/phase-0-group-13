package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.ZombieType;
import model.game.Board;
import model.game.TileEffects.FireEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TileEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import org.junit.jupiter.api.Test;

/**
 * The four attack sets the doc names for Zomboss, one chapter at a time.
 *
 * <p>{@link ZombossActionTest} covers what every chapter shares -- segments, the stun, two rows,
 * moving between lanes. What it does not cover is the half the doc spells out per world: Egypt's
 * missile and charge, the Dark Ages fireball and the rows it sets alight, the mammoth's ice
 * missile and deep freeze, and the beach octopus and its suction. Those were verified by hand and
 * had nothing holding them.
 *
 * <p>No zombie repository is loaded in a unit test, so the paths that summon a minion -- the Imp
 * Dragon out of a fireball, the odd helper zombie -- return early on their own. Everything checked
 * here acts on the board directly and needs nothing but the board.
 */
class ZombossChapterAttacksTest {

  private static final int TOTAL_HP = 18500;
  private static final List<Integer> STAGES = List.of(4000, 8000, 6500);
  private static final int ROWS = 5;
  private static final int COLUMNS = 9;

  /** Long enough for both the frequent attack and the saved-up one to have fired. */
  private static final int LONG_ENOUGH = 700;

  private static Board board() {
    Board board = new Board(ROWS, COLUMNS);
    board.initialize();
    return board;
  }

  private static Plant peashooter(int row, int col) {
    PlantTemplate template = new PlantTemplate();
    template.name = "Peashooter";
    template.baseHp = 300;
    template.cost = 100;
    return new Plant(template, row, col, PlantCategory.SHOOTER,
        EnumSet.noneOf(PlantTag.class), null, null);
  }

  private static int fillLawn(Board board) {
    int planted = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        board.placePlant(peashooter(row, col));
        planted++;
      }
    }
    return planted;
  }

  private static Zombie zomboss(Board board, ZombieType chapter) {
    ZombossAction action =
        new ZombossAction(chapter, new ZombossHealth(STAGES, TOTAL_HP), 10);
    Zombie boss = new Zombie("ZombieZombossMech", TOTAL_HP, 0.01, 1, 8.5, action);
    boss.setBoss(true);
    boss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(boss);
    return boss;
  }

  private static void run(Zombie boss, Board board, int ticks) {
    for (int tick = 0; tick < ticks; tick++) {
      boss.getBehavior().execute(boss, board, tick);
    }
  }

  private static int livingPlants(Board board) {
    int alive = 0;
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        alive++;
      }
    }
    return alive;
  }

  private static int tilesOfType(Board board, Class<? extends TileEffect> kind) {
    int found = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        TileEffect effect = board.getTile(row, col) == null
            ? null : board.getTile(row, col).getEffect();
        if (kind.isInstance(effect)) {
          found++;
        }
      }
    }
    return found;
  }

  // ---- Ancient Egypt: the robot ---------------------------------------------------------

  @Test
  void egyptsMissileDestroysPlantsAndThrowsUpGraves() {
    Board board = board();
    // A grave can only go on an empty tile, so the lawn is left with room for one.
    for (int row = 0; row < ROWS; row++) {
      board.placePlant(peashooter(row, 0));
    }
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_EGYPT);

    run(boss, board, LONG_ENOUGH);

    assertTrue(tilesOfType(board, TombStoneEffect.class) > 0,
        "the doc has the missile creating graves in two cells");
  }

  @Test
  void egyptChargesDownItsOwnTwoRowsAndClearsThem() {
    Board board = board();
    int planted = fillLawn(board);
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_EGYPT);

    run(boss, board, LONG_ENOUGH);

    assertTrue(livingPlants(board) < planted,
        "charging down two rows has to destroy the plants standing in them");
  }

  // ---- Dark Ages: the dragon -------------------------------------------------------------

  @Test
  void theDragonsFireballSetsTheGroundAlight() {
    Board board = board();
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_DARK);

    run(boss, board, LONG_ENOUGH);

    assertTrue(tilesOfType(board, FireEffect.class) > 0,
        "a fireball has to leave the cell it landed on burning");
  }

  @Test
  void theDragonIgnitesTwoWholeRowsOppositeItself() {
    Board board = board();
    fillLawn(board);
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_DARK);

    run(boss, board, LONG_ENOUGH);

    // Two rows is 18 cells; the single fireballs cannot reach that many on their own.
    assertTrue(tilesOfType(board, FireEffect.class) >= 2 * COLUMNS,
        "igniting two rows should leave at least both rows burning");
  }

  // ---- Frostbite Caves: the mammoth ------------------------------------------------------

  @Test
  void theMammothFreezesAColumnSolid() {
    Board board = board();
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_COWBOY);

    run(boss, board, LONG_ENOUGH);

    assertEquals(ROWS, tilesOfType(board, IceTrailEffect.class),
        "freezing a column means every row of that one column");
  }

  @Test
  void theMammothsIceWindFreezesThePlantsInTwoRows() {
    Board board = board();
    fillLawn(board);
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_COWBOY);

    int peakFrozen = 0;
    for (int tick = 0; tick < LONG_ENOUGH; tick++) {
      boss.getBehavior().execute(boss, board, tick);
      int frozen = 0;
      for (Plant plant : board.getPlants()) {
        if (!plant.isDead() && plant.isFrozen(tick)) {
          frozen++;
        }
      }
      peakFrozen = Math.max(peakFrozen, frozen);
    }

    assertTrue(peakFrozen > COLUMNS,
        "an ice wind down two rows should freeze more than a single row's worth of plants");
  }

  @Test
  void theMammothNeitherChangesRowsNorSummonsAnything() {
    Board board = board();
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_COWBOY);
    int startRow = boss.getRow();

    run(boss, board, LONG_ENOUGH);

    assertEquals(startRow, boss.getRow(),
        "the doc gives the mammoth no movement between rows");
    assertEquals(1, board.getZombies().size(),
        "the doc gives the mammoth no zombie spawning");
  }

  // ---- Big Wave Beach: the octopus -------------------------------------------------------

  @Test
  void theBeachOctopusEatsPlantsOffTheLawn() {
    Board board = board();
    int planted = fillLawn(board);
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_PIRATE);

    run(boss, board, LONG_ENOUGH);

    assertTrue(livingPlants(board) < planted,
        "the little octopuses surface and eat a plant each");
  }

  @Test
  void theBeachSuctionDragsOrdinaryZombiesTowardsTheBoss() {
    Board board = board();
    Zombie boss = zomboss(board, ZombieType.ZOMBOSS_PIRATE);
    // Both lanes the suction can pick, so wherever the boss stands one of these is caught.
    Zombie low = new Zombie("basic", 500, 0.0, 0, 1.0, new StandardZombieAction(10));
    Zombie high = new Zombie("basic", 500, 0.0, ROWS - 1, 1.0, new StandardZombieAction(10));
    board.spawnZombie(low);
    board.spawnZombie(high);
    double lowStart = low.getX();
    double highStart = high.getX();

    run(boss, board, LONG_ENOUGH);

    assertTrue(low.getX() > lowStart || high.getX() > highStart || low.isDead() || high.isDead(),
        "suction has to pull zombies in the two opposite rows towards the boss");
  }

  @Test
  void everyChapterLeavesTheBossHoldingTheRightHandColumns() {
    for (ZombieType chapter : new ZombieType[] {ZombieType.ZOMBOSS_EGYPT,
        ZombieType.ZOMBOSS_DARK, ZombieType.ZOMBOSS_COWBOY, ZombieType.ZOMBOSS_PIRATE}) {
      Board board = board();
      Zombie boss = zomboss(board, chapter);

      run(boss, board, LONG_ENOUGH);

      assertTrue(boss.getX() > COLUMNS / 2.0,
          chapter + " should hold the far side of the lawn rather than walk to the house");
    }
  }
}
