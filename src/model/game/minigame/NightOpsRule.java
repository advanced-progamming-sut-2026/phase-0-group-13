package model.game.minigame;

import data.GameDataManager;
import model.game.GameState;
import model.game.plant.PlantParts.PlantTemplate;

public class NightOpsRule extends MiniGame implements SpecialStageRule {
  @Override
  public void apply(GameState gameState) {}

  // فقط گیاهان شبانه (تگ Night) یا قارچ‌ها (که ذاتا شبانه‌ان) قابل کاشتنن
  @Override
  public boolean isPlantAllowed(String plantName) {
    if (GameDataManager.plantRepository == null || plantName == null) {
      return true;
    }
    PlantTemplate template = GameDataManager.plantRepository.find(plantName.toLowerCase());
    if (template == null || template.tags == null) {
      return true;
    }
    String tagsLower = template.tags.toLowerCase();
    return tagsLower.contains("night") || tagsLower.contains("shroom");
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
