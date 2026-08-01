package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

// گیاه‌های تگ wramp-up (Sun-shroom / Kiwibeast) بعد از کاشته شدن مرحله‌به‌مرحله قوی‌تر میشن. این
// کلاس فقط تایمر رشد رو نگه میداره و کار واقعی رو به اکشن مرحله‌ی فعلی میسپاره، پس هر دسته‌ای
// (تولیدکننده‌ی خورشید، ملی، شوتر و ...) بدون تغییر میتونه مرحله‌ای بشه
public class GrowthStageAction implements PlantAction {
  private final PlantAction[] stages;
  private final int[] stageStartTicks; // چند تیک بعد از کاشت وارد هر مرحله میشیم

  private int plantedTick = -1;
  private int currentStage = 0;

  public GrowthStageAction(PlantAction[] stages, int[] stageStartTicks) {
    this.stages = stages;
    this.stageStartTicks = stageStartTicks;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (plantedTick == -1) {
      plantedTick = currentTick;
      plant.setLastActionTick(currentTick);
    }

    // اول مرحله‌ی فعلی کارش رو میکنه بعد ارتقا چک میشه: آستانه‌ی رشد و بازه‌ی عمل معمولا برابرن
    // (Sun-shroom هر دو ۲۴ ثانیه)، و اگه اول ارتقا بدیم مرحله‌ی اول هیچ‌وقت به عمل نمیرسه
    PlantAction activeStage = stages[currentStage];
    if (activeStage != null) {
      activeStage.execute(plant, board, currentTick);
    }

    advanceStageIfDue(plant, currentTick);
  }

  private void advanceStageIfDue(Plant plant, int currentTick) {
    int elapsed = currentTick - plantedTick;
    while (currentStage + 1 < stages.length && elapsed >= stageStartTicks[currentStage + 1]) {
      currentStage++;
      plant.setLastActionTick(currentTick);
      System.out.printf("%s grew to stage %d of %d!%n",
              plant.getName(), currentStage + 1, stages.length);
    }
  }

  public int getCurrentStage() {
    return currentStage + 1;
  }
}
