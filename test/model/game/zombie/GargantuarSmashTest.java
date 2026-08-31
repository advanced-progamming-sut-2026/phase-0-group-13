package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.behavior.GargantuarAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A Gargantuar does not bite, it brings a telephone pole down, and its rig has smash_left for
 * exactly that. The renderer needs to know when the pole actually lands, which is not the same as
 * the zombie being blocked -- it stands over the plant for fifteen ticks between blows.
 */
class GargantuarSmashTest {

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
  }

  @Test
  void theSmashTickIsUnsetUntilThePoleFirstLands() {
    GargantuarAction action = new GargantuarAction(1000);
    assertEquals(-1, action.getSmashTick());
  }

  @Test
  void blockingOnAPlantEventuallyRecordsASmash() {
    Board board = new Board(5, 9);
    PlantFactory plants = new PlantFactory(GameDataManager.plantRepository);
    board.placePlant(plants.createPlant("wall-nut", 2, 3));

    GargantuarAction action = new GargantuarAction(1000);
    Zombie gargantuar = new Zombie("Gargantuar", 1000, 0.0, 2, 3.0, action);
    board.spawnZombie(gargantuar);

    for (int tick = 1; tick <= 60 && action.getSmashTick() < 0; tick++) {
      action.execute(gargantuar, board, tick);
    }
    assertTrue(action.getSmashTick() >= 0,
        "the pole never came down, so the smash clip would never play");
    assertTrue(gargantuar.isEating(),
        "the model still reports it as occupied, which is what Hypno-shroom and the terminal "
            + "board read; only the clip choice changed");
  }
}
