package model.game.minigame;

import model.game.GameState;

public class LastStandRule extends MiniGame implements SpecialStageRule {
  private final int survivalDurationTicks;
  private int elapsedTicks;
  private boolean survived;

  public LastStandRule(int survivalDurationTicks) {
    this.survivalDurationTicks = survivalDurationTicks;
  }

  // پیش‌فرض: ۱۲۰ ثانیه دووم بیار (با نرخ ۱۰ تیک بر ثانیه)
  public LastStandRule() {
    this(1200);
  }

  // این رول فقط شرط برد (دووم آوردن تا پایان زمان) رو تشخیص میده؛ جلوگیری واقعی از افتادن خورشید
  // اضافه یا محدود کردن کارت‌های جدید (که تو Last Stand واقعی هست) نیاز به دسترسی این رول به Board
  // داره که SpecialStageRule فعلا نمیده
  @Override
  public void apply(GameState gameState) {
    elapsedTicks++;
    if (elapsedTicks >= survivalDurationTicks) {
      survived = true;
    }
  }

  @Override
  public boolean checkWinCondition() {
    return survived;
  }
}
