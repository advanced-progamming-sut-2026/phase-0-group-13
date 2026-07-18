package model.game.minigame;

import model.game.GameState;

public class LoveYourPlantsRule extends MiniGame implements SpecialStageRule {
  private final String belovedPlantLower;

  // کل مرحله فقط یه نوع گیاه قابل کاشتنه
  public LoveYourPlantsRule(String belovedPlant) {
    this.belovedPlantLower = belovedPlant == null ? "" : belovedPlant.toLowerCase();
  }

  @Override
  public void apply(GameState gameState) {}

  @Override
  public boolean isPlantAllowed(String plantName) {
    return plantName != null && plantName.toLowerCase().equals(belovedPlantLower);
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
