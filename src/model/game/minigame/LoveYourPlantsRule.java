package model.game.minigame;

import model.game.GameState;

public class LoveYourPlantsRule extends MiniGame implements SpecialStageRule {
  private final String belovedPlantLower;

  // کل مرحله فقط یه نوع گیاه قابل کاشتنه
  public LoveYourPlantsRule(String belovedPlant) {
    this.belovedPlantLower =
            belovedPlant == null ? "" : model.account.User.normalizePlantKey(belovedPlant);
  }

  @Override
  public void apply(GameState gameState) {}

  @Override
  public boolean isPlantAllowed(String plantName) {
    return plantName != null
            && model.account.User.normalizePlantKey(plantName).equals(belovedPlantLower);
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
