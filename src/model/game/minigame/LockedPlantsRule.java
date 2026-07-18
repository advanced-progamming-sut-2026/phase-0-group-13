package model.game.minigame;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.game.GameState;

public class LockedPlantsRule extends MiniGame implements SpecialStageRule {
  private final Set<String> allowedPlantsLower;

  // فقط لیست مشخصی از گیاه‌ها قابل کاشتن هستن، صرف‌نظر از دک انتخابی بازیکن
  public LockedPlantsRule(List<String> allowedPlants) {
    this.allowedPlantsLower = new HashSet<>();
    for (String plant : allowedPlants) {
      if (plant != null) {
        allowedPlantsLower.add(plant.toLowerCase());
      }
    }
  }

  @Override
  public void apply(GameState gameState) {}

  @Override
  public boolean isPlantAllowed(String plantName) {
    return plantName != null && allowedPlantsLower.contains(plantName.toLowerCase());
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
