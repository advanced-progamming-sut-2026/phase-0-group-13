package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.game.zombie.Zombie;
import org.junit.jupiter.api.Test;

/**
 * Plant food is a thing on the lawn now, not a number that goes up on its own.
 *
 * <p>Whatever drops it -- a glowing zombie, a grave -- leaves it lying where it fell, and the
 * player has to click it before it fades. What collecting does is unchanged: it still goes through
 * {@link GameState#addPlantFood()} and is still capped there, so these tests are as much about
 * what did not change as what did.
 */
class PlantFoodPickupTest {

  private static Board lawn() {
    Board board = new Board(5, 9);
    board.getGameState().setSkySunDisabled(true);
    return board;
  }

  @Test
  void aDroppedDoseSitsOnTheLawnAndIsNotCreditedYet() {
    Board board = lawn();
    int before = board.getGameState().getPlantFoodCount();

    board.dropPlantFood(4.0, 2);

    assertEquals(1, board.getPlantFoodDrops().size(), "it should be lying on the lawn");
    assertEquals(before, board.getGameState().getPlantFoodCount(),
        "and nothing should have been handed over until it is picked up");
  }

  @Test
  void clickingItsTilePicksItUp() {
    Board board = lawn();
    board.dropPlantFood(4.0, 2);
    int before = board.getGameState().getPlantFoodCount();

    assertFalse(board.collectPlantFoodAt(0, 4), "a different lane is not where it landed");
    assertFalse(board.collectPlantFoodAt(2, 7), "and neither is a tile down the row");
    assertTrue(board.collectPlantFoodAt(2, 4), "its own tile has to work");

    assertEquals(before + 1, board.getGameState().getPlantFoodCount());
    board.updateAll(0);
    assertTrue(board.getPlantFoodDrops().isEmpty(), "and it comes off the lawn once taken");
  }

  @Test
  void aDoseLeftAloneFadesWithoutEverBeingCredited() {
    Board board = lawn();
    board.dropPlantFood(4.0, 2);

    for (int tick = 0; tick <= PlantFoodDrop.LIFETIME_TICKS; tick++) {
      board.updateAll(tick);
    }

    assertTrue(board.getPlantFoodDrops().isEmpty(), "it should have faded by now");
    assertEquals(0, board.getGameState().getPlantFoodCount(),
        "a dose nobody collected is a dose nobody gets");
  }

  /** The cap is the existing rule; moving the credit to pickup time must not quietly break it. */
  @Test
  void atTheCapTheDoseIsLeftLyingRatherThanThrownAway() {
    Board board = lawn();
    while (board.getGameState().addPlantFood()) {
      // fill up to the cap
    }
    int full = board.getGameState().getPlantFoodCount();
    board.dropPlantFood(4.0, 2);

    assertFalse(board.collectPlantFoodAt(2, 4), "there is nowhere to put it");
    assertEquals(full, board.getGameState().getPlantFoodCount(), "so the cap still holds");
    assertEquals(1, board.getPlantFoodDrops().size(),
        "and it stays on the lawn instead of being swallowed");
  }

  @Test
  void aGlowingZombieLeavesItsDoseOnTheLawnInsteadOfHandingItOver() {
    Board board = lawn();
    Zombie glowing = new Zombie("glowing", 10, 0.0185, 2, 5.0, null);
    glowing.setShiny(true);
    board.spawnZombie(glowing);
    glowing.takeDamage(100, true);

    board.updateAll(0);

    assertEquals(0, board.getGameState().getPlantFoodCount(),
        "killing it must not credit the food by itself");
    assertEquals(1, board.getPlantFoodDrops().size(), "the dose should be on the lawn");
    assertEquals(2, board.getPlantFoodDrops().get(0).getRow(), "in the lane it died in");
  }
}
