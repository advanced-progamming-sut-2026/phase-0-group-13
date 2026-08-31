package model.game.minigame;

import java.util.List;
import model.game.Board;
import model.game.GameState;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.ZombossAction;
import model.game.zombie.behavior.ZombossHealth;

public class BossStageRule implements SpecialStageRule {
  private final String bossName;

  /** داک: مرحله‌های زامباس بر پایهٔ نوار نقاله‌اند. */
  private final ConveyorRule belt;

  private boolean bossAnnounced;
  private boolean bossSeen;
  private boolean bossDefeated;
  private Zombie boss;

  public BossStageRule(String bossName) {
    this(bossName, List.of());
  }

  public BossStageRule(String bossName, List<String> beltPlants) {
    this.bossName = bossName;
    this.belt = new ConveyorRule(beltPlants, BELT_INTERVAL_TICKS);
  }

  /** Faster than the chapter-one belt's 120 ticks. */
  private static final int BELT_INTERVAL_TICKS = 55;

  @Override
  public void apply(GameState gameState) {
    belt.apply(gameState);
  }

  @Override
  public ConveyorRule belt() {
    return belt;
  }

  @Override
  public boolean isPlantAllowed(String plantName) {
    return belt.isPlantAllowed(plantName);
  }

  @Override
  public boolean checkWinCondition(Board board) {
    if (isBossOnField(board)) {
      bossSeen = true;
      if (!bossAnnounced) {
        bossAnnounced = true;
        System.out.println("=== BOSS STAGE: " + bossName + " has rolled onto the lawn! ===");
      }
      return false;
    }

    if (bossSeen && !bossDefeated) {
      bossDefeated = true;
      System.out.println("=== " + bossName + " has been destroyed! The chapter is yours. ===");
    }
    return bossDefeated;
  }

  private boolean isBossOnField(Board board) {
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && bossName.equalsIgnoreCase(zombie.getName())) {
        boss = zombie;
        return true;
      }
    }
    return false;
  }

  public boolean isBossDefeated() {
    return bossDefeated;
  }

  public Zombie getBoss() {
    return boss != null && !boss.isDead() ? boss : null;
  }

  public ZombossAction getBossAction() {
    Zombie current = getBoss();
    return current != null && current.getBehavior() instanceof ZombossAction action ? action : null;
  }

  public ZombossHealth getBossHealth() {
    ZombossAction action = getBossAction();
    return action == null ? null : action.getHealth();
  }

  public boolean isBossStunned() {
    ZombossAction action = getBossAction();
    return action != null && action.isStunned();
  }

  public String getBossName() {
    return bossName;
  }
}
