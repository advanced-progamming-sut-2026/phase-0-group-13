package model.game.minigame;

import java.util.ArrayList;
import java.util.List;
import model.game.GameState;

public class ConveyorRule extends MiniGame implements SpecialStageRule {
  private final List<String> beltPlants;
  private final int spawnIntervalTicks;
  private int elapsedTicks;
  private int nextIndex;
  private String readyPlant;

  public ConveyorRule(List<String> beltPlants, int spawnIntervalTicks) {
    this.beltPlants = new ArrayList<>(beltPlants);
    this.spawnIntervalTicks = spawnIntervalTicks;
  }

  // به‌جای خرج کردن خورشید، هر spawnIntervalTicks یه گیاه جدید (به ترتیب چرخشی روی beltPlants) رو
  // نوار نقاله آماده میشه؛ GamePlayController باید هر تیک consumeReadyPlant رو چک کنه و در صورت غیر
  // null بودن، اون گیاه رو قابل‌کاشت رایگان به بازیکن نشون بده
  @Override
  public void apply(GameState gameState) {
    if (beltPlants.isEmpty()) {
      return;
    }
    elapsedTicks++;
    if (elapsedTicks >= spawnIntervalTicks) {
      elapsedTicks = 0;
      readyPlant = beltPlants.get(nextIndex % beltPlants.size());
      nextIndex++;
    }
  }

  public String consumeReadyPlant() {
    String plant = readyPlant;
    readyPlant = null;
    return plant;
  }

  @Override
  public boolean checkWinCondition() {
    // برد این مرحله معمولا با تموم شدن موج‌ها مشخص میشه (مثل بقیه مراحل عادی)، نه شرط مخصوص خودش
    return false;
  }
}
