package model.game.minigame;

import model.game.Board;
import model.game.GameState;
import model.game.zombie.Zombie;

// لِوِل ۴ هر فصل مرحله‌ی باسه. این قانون فاز باس رو مدیریت میکنه: ورود زامباس رو اعلام میکنه و
// به‌محض کشته شدنش مرحله رو برده اعلام میکنه، حتی اگه چندتا زامبی احضارشده هنوز رو زمین مونده باشن
public class BossStageRule implements SpecialStageRule {
  private final String bossName;

  private boolean bossAnnounced;
  private boolean bossSeen;
  private boolean bossDefeated;

  public BossStageRule(String bossName) {
    this.bossName = bossName;
  }

  @Override
  public void apply(GameState gameState) {
    // مرحله‌ی باس محدودیت اضافه‌ای رو منابع بازیکن نمیذاره
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
        return true;
      }
    }
    return false;
  }

  public boolean isBossDefeated() {
    return bossDefeated;
  }
}
