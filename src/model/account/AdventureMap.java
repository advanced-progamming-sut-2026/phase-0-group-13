package model.account;

import model.Result;

public class AdventureMap {
  public static final int MAX_STAGES = 4;
  public static final int LEVELS_PER_STAGE = 4;

  private static final String[][] LEVEL_REWARDS = {
      {"Repeater", "Bonk Choy", "Grave Buster", "Iceberg Lettuce"},
      {"Hot Potato", "Pepper-pult", "Rotobaga", "Fire Peashooter"},
      {"Lily Pad", "Tangle Kelp", "Sea-shroom", "Bowling Bulb"},
      {"Sun-shroom", "Fume-shroom", "Magnet-shroom", "Hypno-shroom"},
  };

  public static Result getLevelReward(int stage, int level) {
    if (stage == MAX_STAGES && level == LEVELS_PER_STAGE) {
      return new Result(true, "Congratulations! Silver Trophy Unlocked!", "silver_trophy");
    }
    if (stage < 1 || stage > MAX_STAGES || level < 1 || level > LEVELS_PER_STAGE) {
      return new Result(false, "No specific plant reward for this level.", null);
    }
    String plant = LEVEL_REWARDS[stage - 1][level - 1];
    return new Result(true, "Reward Unlocked: " + plant + "!", plant);
  }

  public static String getEnvironmentForStage(int stage) {
    switch (stage) {
      case 1:
        return "ANCIENT_EGYPT";
      case 2:
        return "FROSTBITE_CAVES";
      case 3:
        return "BIG_WAVE_BEACH";
      case 4:
        return "DARK_AGES";
      default:
        return null;
    }
  }
}
