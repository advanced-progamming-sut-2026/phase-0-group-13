package model.account;

import model.Result;

public class AdventureMap {
  public static final int MAX_STAGES = 4;
  public static final int LEVELS_PER_STAGE = 4;

  /**
   * The plant each adventure level unlocks, indexed [stage - 1][level - 1].
   *
   * <p>The doc fixes the reward kinds ("Unlockable: change a plant or a stage from locked to
   * available") but names no plant per level, so the choice here is the chapter's: each row is
   * drawn from the plants that belong to that chapter's season, which is the same season
   * {@link #getEnvironmentForStage} and MatchLauncher use. None of the sixteen is in the six a new
   * account starts with, which is what the table used to get wrong -- chapter 1 handed out
   * Peashooter, Sunflower and Wall-nut, all of which every player already owned, so every one of
   * those rewards fell through to MatchCompletion's "first locked plant" fallback and no level had
   * a reward you could predict.
   *
   * <p>Names are the display names from plants.json; every lookup on the way to the player
   * normalises punctuation and case, so "Grave Buster" and "gravebuster" reach the same template.
   */
  private static final String[][] LEVEL_REWARDS = {
      // Ancient Egypt: tombstones, and the close-range plants that chapter teaches
      {"Repeater", "Bonk Choy", "Grave Buster", "Iceberg Lettuce"},
      // Frostbite Caves: the thaw plants, then fire to answer the ice
      {"Hot Potato", "Pepper-pult", "Rotobaga", "Fire Peashooter"},
      // Big Wave Beach: the water plants, without which that chapter's lanes are unplayable
      {"Lily Pad", "Tangle Kelp", "Sea-shroom", "Bowling Bulb"},
      // Dark Ages: mushrooms, which are the chapter's whole roster
      {"Sun-shroom", "Fume-shroom", "Magnet-shroom", "Hypno-shroom"},
  };

  /**
   * What clearing this level gives, as an unlock id in the Result's object.
   *
   * <p>Clearing the last level of the last chapter finishes the adventure, so that one is the
   * trophy rather than another plant. MatchCompletion decides what to do with either.
   */
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
