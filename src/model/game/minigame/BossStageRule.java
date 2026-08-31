package model.game.minigame;

import java.util.List;
import model.game.Board;
import model.game.GameState;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.ZombossAction;
import model.game.zombie.behavior.ZombossHealth;

// لِوِل ۴ هر فصل مرحله‌ی باسه. این قانون فاز باس رو مدیریت میکنه: ورود زامباس رو اعلام میکنه و
// به‌محض کشته شدنش مرحله رو برده اعلام میکنه، حتی اگه چندتا زامبی احضارشده هنوز رو زمین مونده باشن
public class BossStageRule implements SpecialStageRule {
  private final String bossName;

  /**
   * داک: مرحله‌های زامباس بر پایهٔ نوار نقاله‌اند.
   *
   * <p>Its own belt rather than a second rule, because a match carries one SpecialStageRule and
   * the boss stage needs both halves. Everything that used to ask {@code instanceof ConveyorRule}
   * now asks {@link SpecialStageRule#belt()}, so the belt behaves identically here and on the
   * chapter-one conveyor level.
   */
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

  /**
   * Faster than the chapter-one belt's 120 ticks.
   *
   * <p>The belt is the only source of plants on a boss stage, and Zomboss clears whole rows at a
   * time; at one plant every twelve seconds the player can never rebuild what an ultimate just
   * took, which makes the fight a formality rather than a fight.
   */
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

  /** The boss itself once it has arrived, or null before that and after it goes down. */
  public Zombie getBoss() {
    return boss != null && !boss.isDead() ? boss : null;
  }

  /** Its segment bookkeeping, for the health bar and the stun readout. */
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
