package model.game.minigame.arcade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Wall-nut Bowling's belt, which is a second conveyor separate from the conveyor stage's.
 *
 * <p>Its screen drew a card for every NutType that exists and greyed out all but one, so the belt
 * always showed the whole catalogue whether or not any of it had been delivered.
 */
class BowlingBeltTest {

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
  }

  private static WallnutBowlingEngine engine() {
    return new WallnutBowlingEngine(1, new Random(11));
  }

  @Test
  void theBeltShowsOnlyWhatItHasDelivered() {
    WallnutBowlingEngine engine = engine();
    List<String> queue = engine.getBeltQueue();
    assertFalse(queue.isEmpty(), "the belt delivers one nut at setup");
    assertTrue(queue.size() < WallnutBowlingEngine.NutType.values().length + 1,
        "the belt must not be showing the whole catalogue: " + queue);
  }

  @Test
  void theNutOnOfferIsTheNutThatRolls() {
    WallnutBowlingEngine engine = engine();
    String offered = engine.getReadyNutLabel();
    assertFalse("nothing yet".equals(offered), "a nut is on the belt at setup");

    engine.plantNut(2, 0);
    List<WallnutBowlingEngine.RollingNut> rolling = engine.getRollingNuts();
    assertEquals(1, rolling.size(), "planting should put exactly one nut on the lane");
    assertEquals(offered, rolling.get(0).getType().label,
        "the belt offered one nut and rolled another");
  }

  @Test
  void plantingTakesTheNutOffTheBelt() {
    WallnutBowlingEngine engine = engine();
    int before = engine.getBeltQueue().size();
    engine.plantNut(2, 0);
    assertEquals(before - 1, engine.getBeltQueue().size(),
        "the planted nut should leave the belt");
  }

  @Test
  void anEmptyBeltRefusesToPlant() {
    WallnutBowlingEngine engine = engine();
    while (!engine.getBeltQueue().isEmpty()) {
      engine.plantNut(2, 0);
    }
    String refusal = engine.plantNut(2, 0);
    assertTrue(refusal != null && refusal.startsWith("error:"),
        "with nothing delivered there is nothing to plant, got: " + refusal);
  }

  @Test
  void theBeltRefillsAsTheGameRuns() {
    WallnutBowlingEngine engine = engine();
    while (!engine.getBeltQueue().isEmpty()) {
      engine.plantNut(2, 0);
    }
    for (int tick = 0; tick < 400 && engine.getBeltQueue().isEmpty(); tick++) {
      engine.tick();
    }
    assertFalse(engine.getBeltQueue().isEmpty(), "the belt has to keep feeding the player");
  }
}
