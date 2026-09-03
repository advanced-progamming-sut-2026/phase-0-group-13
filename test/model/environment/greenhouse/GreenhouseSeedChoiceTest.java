package model.environment.greenhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import model.Result;
import model.account.User;
import org.junit.jupiter.api.Test;

/**
 * A greenhouse pot is the player's and takes hours to grow, so what goes in it is their choice.
 * Planting used to roll a seed and tell you afterwards what you had grown.
 */
class GreenhouseSeedChoiceTest {

  private static User player() {
    return new User("gardener", "hash", "g@example.com", "Gardener", "male");
  }

  @Test
  void theHouseSeedIsAlwaysOfferedAndOwnedSeedsComeWithIt() {
    User user = player();
    List<String> seeds = user.getGreenHouse().plantableSeeds(user);
    assertTrue(seeds.contains(GreenHouse.HOUSE_SEED), "marigold is the house seed: " + seeds);
    for (String owned : user.getUnlockedPlants()) {
      assertTrue(seeds.stream().anyMatch(s -> s.equalsIgnoreCase(owned)),
          owned + " is unlocked but was not offered: " + seeds);
    }
  }

  @Test
  void theSeedThatGoesInIsTheSeedThatWasPicked() {
    User user = player();
    List<String> seeds = user.getGreenHouse().plantableSeeds(user);
    String wanted = seeds.get(seeds.size() - 1);

    Result planted = user.getGreenHouse().plantSeed(0, user, wanted);

    assertTrue(planted.success(), planted.message());
    assertEquals(wanted, user.getGreenHouse().getPotAt(0).getPlantedSeedId());
  }

  @Test
  void aSeedThePlayerDoesNotOwnIsRefused() {
    User user = player();
    Result planted = user.getGreenHouse().plantSeed(0, user, "Winter Melon");
    assertFalse(planted.success(), "a seed the player has never unlocked should be refused");
    assertTrue(user.getGreenHouse().getPotAt(0).isEmpty(), "the pot should still be empty");
  }

  @Test
  void plantingWithNoSeedNamedIsRefusedRatherThanRolled() {
    User user = player();
    assertFalse(user.getGreenHouse().plantSeed(0, user, "  ").success());
    assertFalse(user.getGreenHouse().plantSeed(0, user, null).success());
  }
}
