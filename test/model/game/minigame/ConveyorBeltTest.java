package model.game.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The conveyor stage's belt.
 *
 * <p>It used to hold exactly one card: every delivery overwrote the last one, so a player who did
 * not plant in time silently lost it, and only that one card could be planted however many the
 * belt appeared to be carrying.
 */
class ConveyorBeltTest {

  private static ConveyorRule beltOf(String... plants) {
    return new ConveyorRule(List.of(plants), 30);
  }

  @Test
  void deliveredCardsQueueUpInsteadOfReplacingEachOther() {
    ConveyorRule belt = beltOf("Peashooter", "Wall-nut", "Cherry Bomb");
    belt.deliverNow();
    belt.deliverNow();
    belt.deliverNow();
    assertEquals(3, belt.getDeliveredPlants().size(),
        "three deliveries should leave three cards on the belt");
  }

  @Test
  void anyCardOnTheBeltCanBePlanted() {
    ConveyorRule belt = beltOf("Peashooter", "Wall-nut");
    belt.deliverNow();
    belt.deliverNow();
    List<String> onBelt = belt.getDeliveredPlants();
    for (String card : onBelt) {
      assertTrue(belt.isPlantAllowed(card),
          card + " is on the belt but the stage rule refuses to let it be planted");
    }
  }

  @Test
  void aPlantTheBeltNeverDeliveredIsStillRefused() {
    ConveyorRule belt = beltOf("Peashooter");
    belt.deliverNow();
    assertFalse(belt.isPlantAllowed("Doom-shroom"),
        "the belt decides what may be planted; anything else stays locked");
  }

  @Test
  void nothingIsPlantableBeforeTheBeltHasDeliveredAnything() {
    ConveyorRule belt = beltOf("Peashooter");
    assertFalse(belt.isPlantAllowed("Peashooter"), "an empty belt supplies nothing");
    assertTrue(belt.getDeliveredPlants().isEmpty());
  }

  @Test
  void plantingTakesTheCardThatWasPlanted() {
    ConveyorRule belt = beltOf("Peashooter", "Wall-nut", "Cherry Bomb");
    for (int i = 0; i < 3; i++) {
      belt.deliverNow();
    }
    List<String> before = belt.getDeliveredPlants();
    // A card that is on the belt exactly once, so which copy is taken cannot mask the behaviour.
    String unique = before.stream()
        .filter(card -> java.util.Collections.frequency(before, card) == 1)
        .reduce((first, second) -> second)
        .orElseThrow();

    assertNotNull(belt.consume(unique), "the belt was holding that card");
    List<String> after = belt.getDeliveredPlants();
    assertEquals(before.size() - 1, after.size());
    // Popping the head instead would have taken a card the player had not planted.
    assertFalse(after.contains(unique), "the planted card should be the one that left the belt");
    List<String> expected = new java.util.ArrayList<>(before);
    expected.remove(unique);
    assertEquals(expected, after, "the remaining cards should keep their order");
  }

  @Test
  void consumingSomethingNotOnTheBeltChangesNothing() {
    ConveyorRule belt = beltOf("Peashooter");
    belt.deliverNow();
    assertEquals(null, belt.consume("Doom-shroom"));
    assertEquals(1, belt.getDeliveredPlants().size());
  }

  @Test
  void aFullBeltStopsRatherThanDroppingWhatIsOnIt() {
    ConveyorRule belt = beltOf("Peashooter", "Wall-nut");
    for (int i = 0; i < ConveyorRule.BELT_CAPACITY + 8; i++) {
      belt.deliverNow();
    }
    assertEquals(ConveyorRule.BELT_CAPACITY, belt.getDeliveredPlants().size(),
        "the belt fills to capacity and holds");
    assertTrue(belt.isFull());

    belt.consumeReadyPlant();
    belt.deliverNow();
    assertEquals(ConveyorRule.BELT_CAPACITY, belt.getDeliveredPlants().size(),
        "space freed by planting should be refilled");
  }

  @Test
  void theBeltRosterIsNotTheSameThingAsWhatItHasHandedOver() {
    ConveyorRule belt = beltOf("Peashooter", "Wall-nut", "Cherry Bomb");
    assertEquals(3, belt.getBeltPlants().size(), "the roster is what it draws from");
    assertTrue(belt.getDeliveredPlants().isEmpty(),
        "nothing has been delivered yet, so the belt is empty; the UI draws this list");
  }
}
