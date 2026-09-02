package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.enums.StatusEffect;
import model.enums.ZombieType;
import model.game.Board;
import model.game.zombie.Zombie;
import org.junit.jupiter.api.Test;

/**
 * The parts of the boss fight the doc states outright: Zomboss stands in two rows, is stunned each
 * time a health segment goes, moves between rows and spawns zombies -- except the Frostbite Caves
 * mammoth, which the doc says does neither.
 *
 * <p>No zombie repository is loaded in a unit test, so the summoning and minion-spawning paths
 * bail out on their own; what is checked here is the movement and segment behaviour, which needs
 * nothing but the board.
 */
class ZombossActionTest {

  private static final int TOTAL_HP = 18500;
  private static final List<Integer> STAGES = List.of(4000, 8000, 6500);

  private static ZombossHealth health() {
    return new ZombossHealth(STAGES, TOTAL_HP);
  }

  private static Board board() {
    Board board = new Board(5, 9);
    board.initialize();
    return board;
  }

  private static Zombie boss(Board board, ZombossAction action) {
    Zombie zomboss = new Zombie("ZombieZombossMech", TOTAL_HP, 0.01, 2, 9.0, action);
    zomboss.setBoss(true);
    zomboss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(zomboss);
    return zomboss;
  }

  private static Zombie boss(ZombieType chapter, Board board) {
    return boss(board, new ZombossAction(chapter, health(), 10));
  }

  private static void run(Zombie zomboss, Board board, int fromTick, int ticks) {
    for (int tick = fromTick; tick < fromTick + ticks; tick++) {
      zomboss.getBehavior().execute(zomboss, board, tick);
    }
  }

  // ---- two rows -------------------------------------------------------------------------

  @Test
  void itStandsInTwoRowsSoPlantsInEitherCanShootIt() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_EGYPT, board);
    assertEquals(2, zomboss.getRowSpan());
    assertTrue(zomboss.occupiesRow(2));
    assertTrue(zomboss.occupiesRow(3), "the second lane is the whole point");
    assertFalse(zomboss.occupiesRow(1));
    assertFalse(zomboss.occupiesRow(4));
  }

  @Test
  void bothOfItsLanesAreAlwaysOnTheBoard() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = new Zombie("ZombieZombossMech", TOTAL_HP, 0.01, 4, 9.0, action);
    zomboss.setBoss(true);
    zomboss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(zomboss);

    action.execute(zomboss, board, 1);
    assertTrue(zomboss.getBottomRow() < board.getRows(),
        "the bottom lane fell off the board at row " + zomboss.getRow());
  }

  // ---- segments and the stun ------------------------------------------------------------

  @Test
  void losingASegmentStunsIt() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = boss(board, action);

    action.execute(zomboss, board, 1);
    assertFalse(action.isStunned(), "it starts the fight on its feet");

    zomboss.takeDamage(4000, true);
    action.execute(zomboss, board, 2);
    assertTrue(action.isStunned(), "the first segment went and nothing happened");
  }

  @Test
  void theStunWearsOffAndItDoesNotFireAgainWhileItLasts() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = boss(board, action);

    action.execute(zomboss, board, 1);
    zomboss.takeDamage(4000, true);
    action.execute(zomboss, board, 2);

    double heldAt = zomboss.getX();
    int stunTicks = action.getStunTicksLeft();
    assertTrue(stunTicks > 0);
    run(zomboss, board, 3, stunTicks);
    assertEquals(heldAt, zomboss.getX(), 0.0001, "a stunned boss should not move");
    assertFalse(action.isStunned(), "the stun never ended");
  }

  @Test
  void chewingThroughTheSameSegmentOnlyStunsItOnce() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = boss(board, action);

    action.execute(zomboss, board, 1);
    zomboss.takeDamage(4000, true);
    action.execute(zomboss, board, 2);
    run(zomboss, board, 3, action.getStunTicksLeft() + 1);
    assertFalse(action.isStunned());

    // More damage, but not enough to finish the second segment.
    zomboss.takeDamage(500, true);
    action.execute(zomboss, board, 200);
    assertFalse(action.isStunned(), "only a cleared segment is worth a stun");
  }

  @Test
  void everySegmentAfterTheFirstStunsItToo() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = boss(board, action);

    action.execute(zomboss, board, 1);
    zomboss.takeDamage(4000, true);
    action.execute(zomboss, board, 2);
    run(zomboss, board, 3, action.getStunTicksLeft() + 1);

    zomboss.takeDamage(8000, true);
    action.execute(zomboss, board, 300);
    assertTrue(action.isStunned(), "the second segment should have opened it up as well");
  }

  @Test
  void aDeadBossIsNotStunnedItIsJustDead() {
    Board board = board();
    ZombossAction action = new ZombossAction(ZombieType.ZOMBOSS_EGYPT, health(), 10);
    Zombie zomboss = boss(board, action);

    action.execute(zomboss, board, 1);
    zomboss.takeDamage(TOTAL_HP, true);
    action.execute(zomboss, board, 2);
    assertTrue(zomboss.isDead());
    assertFalse(action.isStunned());
  }

  // ---- moving between rows --------------------------------------------------------------

  /** Every lane the boss stood in over the run -- it can wander back, so the end row proves little. */
  private static Set<Integer> rowsVisited(Zombie zomboss, Board board, int ticks) {
    Set<Integer> rows = new HashSet<>();
    rows.add(zomboss.getRow());
    for (int tick = 1; tick <= ticks; tick++) {
      zomboss.getBehavior().execute(zomboss, board, tick);
      rows.add(zomboss.getRow());
    }
    return rows;
  }

  @Test
  void itMovesBetweenRows() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_EGYPT, board);
    assertTrue(rowsVisited(zomboss, board, 900).size() > 1,
        "the boss never left the row it spawned in");
  }

  @Test
  void theFrostbiteMammothStaysInItsRows() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_COWBOY, board);
    assertEquals(Set.of(2), rowsVisited(zomboss, board, 900),
        "the doc says the mammoth does not move between rows");
  }

  @Test
  void theFrostbiteMammothSendsNoRoamingMinions() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_COWBOY, board);
    run(zomboss, board, 1, 900);
    // The doc excludes the mammoth from the summoning every other boss does ("گاهی اوقات، زامباس
    // (جز ماموت) تعدادی زامبی ظاهر می‌کند"), and then gives its own column freeze the job of
    // planting frozen zombies down the iced column. Both hold at once: nothing walks on under its
    // own steam, and anything it does put on the lawn is stuck in the ice it just made.
    List<Zombie> loose = board.getZombies().stream()
        .filter(z -> z != zomboss)
        .filter(z -> !z.getActiveEffects().containsKey(StatusEffect.FROZEN))
        .toList();
    assertTrue(loose.isEmpty(),
        "the mammoth summoned " + loose.size() + " zombies that were not frozen in its column");
  }

  /**
   * The column freeze is the mammoth putting zombies on the lawn, and its rig's glacier_column
   * clips are that move. Under the attacking pose the renderer played its slingshot -- the missile
   * throw -- so icing a column looked exactly like firing at one.
   */
  @Test
  void theColumnFreezeHoldsTheSummoningPose() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_COWBOY, board);
    ZombossAction action = (ZombossAction) zomboss.getBehavior();

    boolean summoned = false;
    for (int tick = 1; tick <= 900 && !summoned; tick++) {
      action.execute(zomboss, board, tick);
      summoned = action.getPose() == ZombossAction.Pose.SUMMONING;
    }
    assertTrue(summoned, "the mammoth never took the summoning pose in 900 ticks");
  }

  // ---- holding station ------------------------------------------------------------------

  @Test
  void itHoldsTheRightHandColumnsInsteadOfWalkingToTheHouse() {
    Board board = board();
    Zombie zomboss = boss(ZombieType.ZOMBOSS_COWBOY, board);
    run(zomboss, board, 1, 2000);
    assertTrue(zomboss.getX() > 1.0,
        "the boss walked off to the house at column " + zomboss.getX());
  }
}
