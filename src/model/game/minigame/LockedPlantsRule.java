package model.game.minigame;

import data.GameDataManager;
import model.game.GameState;

public class LockedPlantsRule extends MiniGame implements SpecialStageRule {
  private final String lockedFamily;
  private final String survivorKey;

  // طبق داک: از یک خانواده فقط یک گیاه در دسترس میمونه و بقیه‌ی اون خانواده قفلن
  public LockedPlantsRule(String lockedFamily, String survivor) {
    this.lockedFamily = lockedFamily.toLowerCase();
    this.survivorKey = model.account.User.normalizePlantKey(survivor);
  }

  @Override
  public void apply(GameState gameState) {}

  @Override
  public boolean restrictsSelection() { return true; }
  @Override
  public boolean isPlantAllowed(String plantName) {
    if (plantName == null || GameDataManager.plantRepository == null
            || model.account.User.normalizePlantKey(plantName).equals(survivorKey)) {
      return true;}
    var template = GameDataManager.plantRepository.find(plantName);
    return template == null || template.tags == null
            || !template.tags.toLowerCase().contains(lockedFamily);
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
