package model.game.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import model.enums.ZombieType;
import model.game.Board;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import model.game.zombie.behavior.ZombossAction;
import model.game.zombie.behavior.ZombossHealth;
import org.junit.jupiter.api.Test;

/**
 * The stage rule around the fight: the doc says Zomboss stages "are conveyor-belt based", and the
 * level is over when the boss is, not when the zombies it dropped are finally cleaned up.
 */
class BossStageRuleTest {

  private static final String BOSS = "ZombieZombossMechEgypt";
  private static final int BOSS_HP = 18500;

  private static Board board() {
    Board board = new Board(5, 9);
    board.initialize();
    return board;
  }

  private static Zombie zomboss(Board board) {
    Zombie boss = new Zombie(BOSS, BOSS_HP, 0.0, 2, 7.0,
        new ZombossAction(ZombieType.ZOMBOSS_EGYPT,
            new ZombossHealth(List.of(4000, 8000, 6500), BOSS_HP), 10));
    boss.setBoss(true);
    boss.setRowSpan(ZombossAction.ROW_SPAN);
    board.spawnZombie(boss);
    return boss;
  }

  @Test
  void aBossStageRunsOnTheConveyorBelt() {
    BossStageRule rule = new BossStageRule(BOSS, List.of("peashooter", "sunflower"));
    assertNotNull(rule.belt(), "the doc says boss stages are conveyor-belt based");
  }

  @Test
  void theBeltIsWhatDecidesWhatCanBePlanted() {
    BossStageRule rule = new BossStageRule(BOSS, List.of("peashooter"));
    rule.belt().deliverNow();
    assertTrue(rule.isPlantAllowed("peashooter"), "the plant on the belt has to be plantable");
    assertFalse(rule.isPlantAllowed("wallnut"), "and nothing else should be");
  }

  @Test
  void theBeltKeepsTickingWithTheStage() {
    BossStageRule rule = new BossStageRule(BOSS, List.of("peashooter"));
    Board board = board();
    for (int tick = 0; tick < 200; tick++) {
      rule.apply(board.getGameState());
    }
    assertNotNull(rule.belt().peekReadyPlant(), "the belt never delivered anything");
  }

  @Test
  void theStageIsNotWonBeforeTheBossHasEvenArrived() {
    BossStageRule rule = new BossStageRule(BOSS, List.of());
    Board board = board();
    assertFalse(rule.checkWinCondition(board));
    assertFalse(rule.isBossDefeated());
  }

  @Test
  void theStageIsNotWonWhileTheBossIsStanding() {
    BossStageRule rule = new BossStageRule(BOSS, List.of());
    Board board = board();
    zomboss(board);
    assertFalse(rule.checkWinCondition(board));
  }

  @Test
  void killingTheBossWinsTheStageEvenWithItsMinionsStillOnTheLawn() {
    BossStageRule rule = new BossStageRule(BOSS, List.of());
    Board board = board();
    Zombie boss = zomboss(board);
    board.spawnZombie(new Zombie("imp", 100, 0.01, 0, 8.0, new StandardZombieAction(10)));

    assertFalse(rule.checkWinCondition(board));
    boss.takeDamage(BOSS_HP, true);
    assertTrue(rule.checkWinCondition(board), "the chapter ends when the boss does");
    assertTrue(rule.isBossDefeated());
  }

  @Test
  void itHandsOutTheBossHealthTheHudDraws() {
    BossStageRule rule = new BossStageRule(BOSS, List.of());
    Board board = board();
    Zombie boss = zomboss(board);
    rule.checkWinCondition(board);

    assertNotNull(rule.getBoss());
    assertNotNull(rule.getBossHealth());
    assertEquals(ZombossHealth.SEGMENTS,
        rule.getBossHealth().segmentsLeft(boss.getCurrentHealth()));

    boss.takeDamage(BOSS_HP, true);
    rule.checkWinCondition(board);
    assertNull(rule.getBoss(), "a dead boss has no bar to draw");
  }
}
