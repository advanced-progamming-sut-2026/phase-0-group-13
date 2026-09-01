package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.List;
import model.enums.StatusEffect;
import model.enums.ZombieType;
import model.game.Board;
import model.game.BossHazard;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What the Zombosses actually put on the lawn.
 *
 * <p>Every one of these attacks used to be a {@code printf} and a dead plant on the same tick: the
 * missile never existed, the boulder never fell, there were no sharks at all and the deep freeze
 * only worked on zombies that happened to already be walking. Driving the behaviour directly is
 * the only way to see them, since a boss stage takes several waves to reach and a match can end
 * before it gets there.
 */
class ZombossAttacksTest {

  private static final int ROWS = 5;
  private static final int COLUMNS = 9;

  /** Long enough for the attack timer (90) and the ultimate (320) to both come round. */
  private static final int LONG_ENOUGH = 700;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
  }

  private static Zombie boss(Board board, ZombieType chapter) {
    ZombossAction action = new ZombossAction(chapter,
        new ZombossHealth(List.of(1000, 1000, 1000), 3000), 100);
    Zombie zomboss = new Zombie("Zomboss", 3000, 0.02, 0, COLUMNS - 2.0, action);
    zomboss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(zomboss);
    return zomboss;
  }

  private static void fillLawn(Board board) {
    PlantFactory plants = new PlantFactory(GameDataManager.plantRepository);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < 4; col++) {
        board.placePlant(plants.createPlant("wall-nut", row, col));
      }
    }
  }

  private static long countOf(Board board, BossHazard.Kind kind) {
    return board.getBossHazards().stream().filter(h -> h.getKind() == kind).count();
  }

  /** Ticks the boss alone, so the hazards stay on the board instead of being advanced away. */
  private static void driveBoss(Zombie zomboss, Board board, int ticks) {
    for (int tick = 1; tick <= ticks; tick++) {
      zomboss.getBehavior().execute(zomboss, board, tick);
    }
  }

  @Test
  void egyptPutsItsMissileInTheAirInsteadOfLandingItStraightAway() {
    Board board = new Board(ROWS, COLUMNS);
    fillLawn(board);
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_EGYPT);
    driveBoss(zomboss, board, LONG_ENOUGH);

    assertTrue(countOf(board, BossHazard.Kind.MISSILE) > 0,
        "the boss fired but nothing was ever in the air");
    BossHazard missile = board.getBossHazards().stream()
        .filter(h -> h.getKind() == BossHazard.Kind.MISSILE).findFirst().orElseThrow();
    assertTrue(missile.isFalling(), "a missile falls onto its tile");
    assertTrue(missile.fallFraction() > 0, "it has not arrived yet, so it is still above the lawn");
    assertFalse(missile.hasLanded(), "it landed on the tick it was fired, which is the old bug");
  }

  @Test
  void aMissileOnlyDoesItsDamageWhenItArrives() {
    Board board = new Board(ROWS, COLUMNS);
    PlantFactory plants = new PlantFactory(GameDataManager.plantRepository);
    board.placePlant(plants.createPlant("wall-nut", 2, 3));
    board.addBossHazard(BossHazard.missile(2, 3, 10));

    for (int tick = 1; tick <= 9; tick++) {
      board.updateAll(tick);
      assertNotNull(board.getPlantAt(2, 3), "the plant died before the missile got there");
      assertFalse(board.getPlantAt(2, 3).isDead(), "the plant died before the missile got there");
    }
    for (int tick = 10; tick <= 14; tick++) {
      board.updateAll(tick);
    }
    assertTrue(board.getPlantAt(2, 3) == null || board.getPlantAt(2, 3).isDead(),
        "the missile arrived and the plant survived it");
    assertTrue(board.getBossHazards().isEmpty(), "a spent missile stayed on the board");
  }

  @Test
  void theMammothSlingsABoulderAndTheBoulderFreezesWhereItLands() {
    Board board = new Board(ROWS, COLUMNS);
    PlantFactory plants = new PlantFactory(GameDataManager.plantRepository);
    board.placePlant(plants.createPlant("wall-nut", 1, 2));
    board.addBossHazard(BossHazard.iceBoulder(1, 2, 5));
    for (int tick = 1; tick <= 8; tick++) {
      board.updateAll(tick);
    }
    assertTrue(board.getPlantAt(1, 2) == null || board.getPlantAt(1, 2).isDead(),
        "the boulder landed on the plant and left it standing");
    assertNotNull(board.getTile(1, 2).getEffect(), "the tile it hit was not frozen over");
  }

  @Test
  void theBeachBossSendsSharksThatHaveToSwimToWhatTheyEat() {
    Board board = new Board(ROWS, COLUMNS);
    fillLawn(board);
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_PIRATE);
    driveBoss(zomboss, board, LONG_ENOUGH);

    assertTrue(countOf(board, BossHazard.Kind.SHARK) > 0, "no sharks were ever let loose");
    BossHazard shark = board.getBossHazards().stream()
        .filter(h -> h.getKind() == BossHazard.Kind.SHARK).findFirst().orElseThrow();
    assertFalse(shark.isFalling(), "a shark swims, it does not fall out of the sky");
    double startedAt = shark.getColumn();
    shark.advance();
    assertTrue(shark.getColumn() < startedAt, "the shark is not moving towards the house");
    assertEquals(startedAt, shark.getPreviousColumn(), 1e-9,
        "previousColumn must hold last tick's place or the view has nothing to tween");
  }

  @Test
  void aSharkEatsThePlantItSwimsIntoAndThenIsGone() {
    Board board = new Board(ROWS, COLUMNS);
    PlantFactory plants = new PlantFactory(GameDataManager.plantRepository);
    board.placePlant(plants.createPlant("wall-nut", 3, 2));
    board.addBossHazard(BossHazard.shark(3, 6.0, 0.5));

    for (int tick = 1; tick <= 40 && !board.getBossHazards().isEmpty(); tick++) {
      board.updateAll(tick);
    }
    assertTrue(board.getPlantAt(3, 2) == null || board.getPlantAt(3, 2).isDead(),
        "the shark swam past its lunch");
    assertTrue(board.getBossHazards().isEmpty(), "the shark stayed on the board after eating");
  }

  /**
   * The mammoth's deep freeze works on the zombies already walking, and it brings none of its own.
   *
   * <p>Worth stating here because the obvious reading of the rubric line -- "frozen zombies" under
   * the ice-caves boss -- is that the glacier columns produce them, and they do not:
   * ZombossActionTest and ZombossChapterAttacksTest both pin the mammoth to summoning nothing.
   * The frozen zombies are the ones the player was already fighting.
   */
  @Test
  void theDeepFreezeCatchesTheZombiesAlreadyOnTheLawnAndBringsNoneOfItsOwn() {
    Board board = new Board(ROWS, COLUMNS);
    fillLawn(board);
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_COWBOY);
    // One per column: the deep freeze picks a column at random, so a row of walkers all standing
    // in the same one turns this assertion into a coin toss.
    for (int col = 0; col < COLUMNS; col++) {
      board.spawnZombie(new Zombie("Walker", 200, 0.0, 0, col, null));
    }
    int before = board.getZombies().size();

    driveBoss(zomboss, board, LONG_ENOUGH);

    assertEquals(before, board.getZombies().size(),
        "the mammoth summoned something, and the doc gives it no zombie spawning at all");
    assertTrue(board.getZombies().stream()
            .anyMatch(z -> z.getActiveEffects().containsKey(StatusEffect.FROZEN)),
        "the deep freeze caught nobody");
  }

  /** A charging boss goes through its own zombies, not only through the plants. */
  @Test
  void theEgyptChargeTramplesWhateverIsInFrontOfIt() {
    Board board = new Board(ROWS, COLUMNS);
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_EGYPT);
    // One per row: the boss stomps between rows every few hundred ticks, so which lane it ends up
    // charging down is not something the test gets to choose.
    List<Zombie> bystanders = new java.util.ArrayList<>();
    for (int row = 0; row < ROWS; row++) {
      Zombie bystander = new Zombie("Bystander", 200, 0.0, row, COLUMNS - 3.0, null);
      bystanders.add(bystander);
      board.spawnZombie(bystander);
    }

    driveBoss(zomboss, board, LONG_ENOUGH);
    assertTrue(bystanders.stream().anyMatch(Zombie::isDead),
        "the boss charged straight through them and every one walked away");
  }

  /** moveTo and not setX: the view tweens previousX -> x, and setX collapses the two together. */
  @Test
  void aChargingBossLeavesTheViewSomethingToTweenBetween() {
    Board board = new Board(ROWS, COLUMNS);
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_EGYPT);
    ZombossAction action = (ZombossAction) zomboss.getBehavior();

    boolean sawTravel = false;
    for (int tick = 1; tick <= LONG_ENOUGH; tick++) {
      action.execute(zomboss, board, tick);
      if (zomboss.getX() != zomboss.getPreviousX()) {
        sawTravel = true;
      }
    }
    assertTrue(sawTravel,
        "the boss never once had a previous place different from its current one, so every step "
            + "it took was drawn as a jump");
  }

  /**
   * The turbine used to flatten both rows on the tick it switched on, so nothing was ever seen
   * being pulled in: the plants were gone before the suction clip had a frame on screen.
   */
  @Test
  void theTurbineTakesTheRowApartOverTimeRatherThanAllAtOnce() {
    Board board = new Board(ROWS, COLUMNS);
    fillLawn(board);
    int before = (int) board.getPlants().stream().filter(p -> !p.isDead()).count();
    Zombie zomboss = boss(board, ZombieType.ZOMBOSS_PIRATE);
    ZombossAction action = (ZombossAction) zomboss.getBehavior();

    int firstSuctionTick = -1;
    for (int tick = 1; tick <= LONG_ENOUGH; tick++) {
      action.execute(zomboss, board, tick);
      int alive = (int) board.getPlants().stream().filter(p -> !p.isDead()).count();
      if (alive < before && firstSuctionTick < 0) {
        firstSuctionTick = tick;
        assertTrue(before - alive <= 2,
            "two whole rows went in one tick, which is the instant clearing this replaced");
      }
    }
    assertTrue(firstSuctionTick > 0, "the turbine never took anything");
  }
}
