package model.game.zombie.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.enums.ZombieType;
import java.util.List;
import model.game.Board;
import model.game.zombie.Zombie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The boss summoning needed a pose of its own.
 *
 * <p>Every chapter's rig draws the summon differently and none of them look like firing: Egypt
 * tears open a portal, the pirate has a spawn, the mammoth raises a column of ice. They were all
 * sitting unused because the boss only ever reported IDLE, MOVING, ATTACKING or STUNNED, so a
 * summon was drawn as whatever the boss happened to be doing anyway.
 */
class ZombossSummonPoseTest {

  private static final List<Integer> STAGES = List.of(4000, 8000, 6500);
  private static final int TOTAL_HP = 18500;

  @BeforeAll
  static void loadTheGameData() {
    // summonMinion builds a real minion off the repository; without it the summon is a no-op.
    new GameDataManager();
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

  /** Egypt is a summoning chapter; the mammoth is the one that never calls anything in. */
  @Test
  void aSummoningChapterReachesTheSummonPose() {
    Board board = board();
    ZombossAction action =
        new ZombossAction(ZombieType.ZOMBOSS_EGYPT, new ZombossHealth(STAGES, TOTAL_HP), 10);
    Zombie zomboss = boss(board, action);

    boolean summoned = false;
    for (int tick = 1; tick <= 400 && !summoned; tick++) {
      action.execute(zomboss, board, tick);
      summoned = action.getPose() == ZombossAction.Pose.SUMMONING;
    }
    assertTrue(summoned, "the boss never reported SUMMONING, so its portal clip can never play");
  }

  @Test
  void theSummonPoseIsHeldRatherThanLastingASingleTick() {
    Board board = board();
    ZombossAction action =
        new ZombossAction(ZombieType.ZOMBOSS_EGYPT, new ZombossHealth(STAGES, TOTAL_HP), 10);
    Zombie zomboss = boss(board, action);

    int firstSummonTick = -1;
    for (int tick = 1; tick <= 400; tick++) {
      action.execute(zomboss, board, tick);
      if (action.getPose() == ZombossAction.Pose.SUMMONING) {
        firstSummonTick = tick;
        break;
      }
    }
    assertTrue(firstSummonTick > 0, "test setup: no summon happened");

    int held = 0;
    for (int tick = firstSummonTick + 1; tick <= firstSummonTick + 30; tick++) {
      action.execute(zomboss, board, tick);
      if (action.getPose() == ZombossAction.Pose.SUMMONING) {
        held++;
      } else {
        break;
      }
    }
    assertTrue(held >= 5,
        "the summon pose lasted " + (held + 1) + " ticks; a wind-up clip cut that short shows "
            + "nothing at all");
  }

  @Test
  void theMammothNeverSummonsAndSoNeverTakesThePose() {
    Board board = board();
    ZombossAction action =
        new ZombossAction(ZombieType.ZOMBOSS_COWBOY, new ZombossHealth(STAGES, TOTAL_HP), 10);
    Zombie zomboss = boss(board, action);

    for (int tick = 1; tick <= 400; tick++) {
      action.execute(zomboss, board, tick);
      assertEquals(false, action.getPose() == ZombossAction.Pose.SUMMONING,
          "the mammoth summons nothing, so it should never take the summon pose");
    }
  }
}
