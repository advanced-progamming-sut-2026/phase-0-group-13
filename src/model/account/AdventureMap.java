package model.account;

import model.Result;

public class AdventureMap {
  public static final int MAX_STAGES = 4;
  public static final int LEVELS_PER_STAGE = 4;

  // این و باید فکر بکنیم و درست بکنیم ، فعلا فصل هارو انتخاب نکردیم به صورت دقیق که بخوایم ریوارد
  // هاشو مشخص بکنیم
  // عشق
  public static Result getLevelReward(int stage, int level) {
    if (stage == 1) {
      switch (level) {
        // فعلا راجب مراحل بعدی فکر نکردیم که چه فصلایی باشه ، اینا فعلا چیزای بیسیکه
        case 1:
          return new Result(true, "Reward Unlocked: Peashooter!", "peashooter");
        case 2:
          return new Result(true, "Reward Unlocked: Sunflower!", "sunflower");
        case 3:
          return new Result(true, "Reward Unlocked: Cherry Bomb!", "cherrybomb");
        case 4:
          return new Result(true, "Reward Unlocked: Wall-nut!", "wall-nut");
      }
    }

    if (stage == MAX_STAGES && level == LEVELS_PER_STAGE) {
      return new Result(true, "Congratulations! Silver Trophy Unlocked!", "silver_trophy");
    }

    return new Result(false, "No specific plant reward for this level.", null);
  }

  /**
   * The season a chapter is set in. This used to answer "DAY"/"NIGHT" for the first two stages and
   * null for the rest, which was left over from before the map became four seasons; it now matches
   * the stage-to-season mapping MatchLauncher and the adventure screen already use.
   */
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
