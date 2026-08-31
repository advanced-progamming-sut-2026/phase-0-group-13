package model.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import model.enums.StatusEffect;
import model.enums.ZombieType;
import model.game.TileEffects.IceTrailEffect;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import model.game.zombie.behavior.ZombossAction;
import model.game.zombie.behavior.ZombossHealth;
import org.junit.jupiter.api.Test;

/**
 * "Zomboss occupies two rows (i.e. plants in two rows can attack it)."
 *
 * <p>Which means the board has to agree in both directions: a plant in either lane has to see a
 * target there, and a shot fired down either lane has to land. Everything else on the lawn stands
 * in exactly one row and must keep doing so, which is the other half of what is checked here.
 */
class BossOccupiesTwoRowsTest {

  private static final int TOP_ROW = 2;
  private static final int BOSS_HP = 18500;
  private static final int DAMAGE = 20;

  private static Board board() {
    Board board = new Board(5, 9);
    board.initialize();
    return board;
  }

  private static Zombie zomboss(Board board) {
    Zombie boss = new Zombie("ZombieZombossMechEgypt", BOSS_HP, 0.0, TOP_ROW, 7.0,
        new ZombossAction(ZombieType.ZOMBOSS_EGYPT,
            new ZombossHealth(List.of(4000, 8000, 6500), BOSS_HP), 10));
    boss.setBoss(true);
    boss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(boss);
    return boss;
  }

  private static void fireDownRow(Board board, int row, int shots) {
    int tick = 0;
    for (int i = 0; i < shots; i++) {
      board.addProjectile(new Projectile(DAMAGE, 0.5, 1, row, false, false));
      for (int step = 0; step < 20; step++) {
        board.updateAll(tick++);
      }
    }
  }

  @Test
  void aPlantInEitherLaneSeesItAsATarget() {
    Board board = board();
    zomboss(board);
    assertTrue(board.hasZombieInRow(TOP_ROW, 0), "the top lane cannot see the boss");
    assertTrue(board.hasZombieInRow(TOP_ROW + 1, 0), "the bottom lane cannot see the boss");
    assertFalse(board.hasZombieInRow(TOP_ROW - 1, 0), "it is not in the lane above");
    assertFalse(board.hasZombieInRow(TOP_ROW + 2, 0), "it is not in the lane below");
  }

  @Test
  void shotsFiredDownTheTopLaneHitIt() {
    Board board = board();
    Zombie boss = zomboss(board);
    int before = boss.getCurrentHealth();
    fireDownRow(board, TOP_ROW, 3);
    assertTrue(boss.getCurrentHealth() < before, "shots down the top lane did nothing");
  }

  @Test
  void shotsFiredDownTheSecondLaneHitItToo() {
    Board board = board();
    Zombie boss = zomboss(board);
    int before = boss.getCurrentHealth();
    fireDownRow(board, TOP_ROW + 1, 3);
    assertTrue(boss.getCurrentHealth() < before, "the second lane could not touch the boss");
  }

  @Test
  void shotsInAnUnoccupiedLaneStillMissIt() {
    Board board = board();
    Zombie boss = zomboss(board);
    int before = boss.getCurrentHealth();
    fireDownRow(board, TOP_ROW - 1, 3);
    assertTrue(boss.getCurrentHealth() == before, "a lane it is not in should miss");
  }

  @Test
  void anOrdinaryZombieIsStillOnlyInOneRow() {
    Board board = board();
    Zombie walker = new Zombie("basic", 200, 0.0, TOP_ROW, 6.0, new StandardZombieAction(10));
    board.spawnZombie(walker);
    assertTrue(walker.occupiesRow(TOP_ROW));
    assertFalse(walker.occupiesRow(TOP_ROW + 1), "only Zomboss spans two lanes");
    assertFalse(board.hasZombieInRow(TOP_ROW + 1, 0));
  }

  @Test
  void aMowerRollsStraightUnderTheRobotWithoutCrushingIt() {
    Board board = board();
    Zombie boss = zomboss(board);
    Lawnmower mower = new Lawnmower(TOP_ROW);
    mower.trigger();
    for (int tick = 0; tick < 200 && mower.isActive(); tick++) {
      mower.move(board.getZombies());
    }
    assertFalse(boss.isDead(), "a lawn mower should not be able to kill Dr. Zomboss");
  }

  @Test
  void groundIceNeitherFreezesTheRobotNorSlidesItOffTheBoard() {
    Board board = board();
    Zombie boss = zomboss(board);
    // The Frostbite boss ices whole columns, its own included, and slip ice would shove a
    // two-lane sprite half off the lawn.
    for (int row = 0; row < board.getRows(); row++) {
      board.placeTileEffect(row, 7, new IceTrailEffect(200, 0.0, true));
    }
    board.placeTileEffect(TOP_ROW, 7, new IceTrailEffect(200, 0.5, false, 1));
    for (int tick = 0; tick < 30; tick++) {
      board.updateAll(tick);
    }
    assertFalse(boss.getActiveEffects().containsKey(StatusEffect.FROZEN),
        "the boss froze itself in its own ice");
    assertTrue(boss.getBottomRow() < board.getRows(),
        "the boss slid until its lower lane left the board");
  }

  @Test
  void aChargingBossDoesNotBurnTheRowsMower() {
    Board board = board();
    Zombie boss = zomboss(board);
    boss.setX(-0.2);
    for (int tick = 0; tick < 5; tick++) {
      board.updateAll(tick);
    }
    for (Lawnmower mower : board.getLawnmowers()) {
      assertTrue(mower.isAvailable(),
          "the boss set off the mower in row " + (mower.getRow() + 1));
    }
    assertFalse(board.isPlayerLost(), "the boss reaching the left edge is not a loss on its own");
  }
}
