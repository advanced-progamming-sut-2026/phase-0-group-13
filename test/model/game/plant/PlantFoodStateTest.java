package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The signal the plant-food animation hangs off.
 *
 * <p>Every plant rig ships a plantfood clip and none of them were ever played, because nothing
 * asked the plant whether a dose was actually running. hasPlantFoodEffect() only says the plant
 * has an effect at all, which is true from the moment it is planted, so it cannot be the trigger.
 */
class PlantFoodStateTest {

  private static PlantFactory factory;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    factory = new PlantFactory(GameDataManager.plantRepository);
  }

  @Test
  void aFreshPlantIsNotUnderPlantFood() {
    Plant peashooter = factory.createPlant("peashooter", 2, 2);
    assertTrue(peashooter.hasPlantFoodEffect(), "a peashooter does have an effect to trigger");
    assertFalse(peashooter.isPlantFoodActive(), "but nothing has triggered it yet");
  }

  @Test
  void applyingPlantFoodTurnsTheSignalOn() {
    Plant peashooter = factory.createPlant("peashooter", 2, 2);
    peashooter.applyPlantFood();
    assertTrue(peashooter.isPlantFoodActive());
  }

  @Test
  void theSignalGoesOffAgainOnceTheDoseHasRunItsCourse() {
    Board board = new Board(5, 9);
    Plant peashooter = factory.createPlant("peashooter", 2, 2);
    board.placePlant(peashooter);
    peashooter.applyPlantFood();
    assertTrue(peashooter.isPlantFoodActive(), "test setup: the dose is running");

    for (int tick = 1; tick <= 600 && peashooter.isPlantFoodActive(); tick++) {
      peashooter.update(tick, board);
    }
    assertFalse(peashooter.isPlantFoodActive(),
        "the dose never ended, so the clip would play for the rest of the match");
  }

  @Test
  void aSingleUsePlantNeverReportsAnActiveDose() {
    Plant cherry = factory.createPlant("cherry-bomb", 2, 2);
    assertFalse(cherry.hasPlantFoodEffect(), "plants.json marks it None (single-use plant)");
    cherry.applyPlantFood();
    assertFalse(cherry.isPlantFoodActive());
  }
}
