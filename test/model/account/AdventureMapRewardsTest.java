package model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import data.persistence.DataPath;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Result;
import org.junit.jupiter.api.Test;

/**
 * Every adventure level has to hand out something, and it has to be something the player does not
 * already have.
 *
 * <p>The table used to cover chapter 1 only, and even those four were plants a new account is
 * created holding, so fifteen of the sixteen levels ended up on MatchCompletion's "first locked
 * plant" fallback. These read the roster off disk so a reward naming a plant that does not exist
 * -- or one of the six starters -- fails here rather than silently becoming the fallback again.
 */
class AdventureMapRewardsTest {

  /** The plants User's constructor grants, so a reward naming one of them is already owned. */
  private static final Set<String> STARTERS = new HashSet<>(List.of(
      "peashooter", "sunflower", "wallnut", "potatomine", "cabbagepult", "puffshroom"));

  private static Set<String> rosterKeys() throws IOException {
    Path path = DataPath.getInstance().getPath("plants");
    assertNotNull(path, "DataPath has no entry for the plant roster");
    assertTrue(Files.exists(path), "plant roster not found at " + path.toAbsolutePath());
    Set<String> names = new HashSet<>();
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonArray plants = new Gson().fromJson(reader, JsonArray.class);
      for (JsonElement element : plants) {
        JsonElement name = element.getAsJsonObject().get("Name");
        if (name != null) {
          names.add(User.normalizePlantKey(name.getAsString()));
        }
      }
    }
    assertEquals(69, names.size(), "the plant roster changed size");
    return names;
  }

  @Test
  void everyLevelOfEveryChapterHasAReward() {
    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
        Result reward = AdventureMap.getLevelReward(stage, level);
        assertTrue(reward.success(), "level " + stage + "-" + level + " has no reward");
        assertTrue(reward.getObject() instanceof String,
            "level " + stage + "-" + level + " carries no unlock id");
        assertFalse(((String) reward.getObject()).isBlank(),
            "level " + stage + "-" + level + " carries a blank unlock id");
      }
    }
  }

  @Test
  void everyPlantRewardIsARealPlantTheAccountDoesNotStartWith() throws IOException {
    Set<String> roster = rosterKeys();
    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
        String id = (String) AdventureMap.getLevelReward(stage, level).getObject();
        if (id.contains("trophy")) {
          continue;
        }
        String key = User.normalizePlantKey(id);
        String where = "level " + stage + "-" + level + " rewards '" + id + "'";
        assertTrue(roster.contains(key), where + ", which is not in plants.json");
        assertFalse(STARTERS.contains(key), where + ", which every account already owns");
      }
    }
  }

  @Test
  void noPlantIsHandedOutTwice() {
    List<String> seen = new ArrayList<>();
    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
        String id = (String) AdventureMap.getLevelReward(stage, level).getObject();
        assertFalse(seen.contains(User.normalizePlantKey(id)),
            "level " + stage + "-" + level + " repeats the reward '" + id + "'");
        seen.add(User.normalizePlantKey(id));
      }
    }
    assertEquals(AdventureMap.MAX_STAGES * AdventureMap.LEVELS_PER_STAGE, seen.size());
  }

  @Test
  void clearingTheAdventureEndsOnTheTrophy() {
    Result last = AdventureMap.getLevelReward(AdventureMap.MAX_STAGES,
        AdventureMap.LEVELS_PER_STAGE);
    assertEquals("silver_trophy", last.getObject());
  }

  @Test
  void aLevelOffTheMapHasNoReward() {
    assertFalse(AdventureMap.getLevelReward(0, 1).success());
    assertFalse(AdventureMap.getLevelReward(1, 0).success());
    assertFalse(AdventureMap.getLevelReward(AdventureMap.MAX_STAGES + 1, 1).success());
    assertFalse(AdventureMap.getLevelReward(1, AdventureMap.LEVELS_PER_STAGE + 1).success());
  }

  @Test
  void walkingTheWholeMapUnlocksFifteenNewPlants() {
    User user = new User("adventurer", "hash", "a@b.c", "nick", "male");
    int owned = user.getUnlockedPlants().size();
    assertEquals(6, owned, "a new account should start with the six starter plants");

    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
        String id = (String) AdventureMap.getLevelReward(stage, level).getObject();
        if (id.contains("trophy")) {
          continue;
        }
        assertFalse(user.hasUnlockedPlant(id),
            "level " + stage + "-" + level + " rewards a plant the player already has by then");
        assertTrue(user.unlockPlant(id).success(), "could not unlock " + id);
      }
    }
    assertEquals(owned + 15, user.getUnlockedPlants().size());
  }
}
