package model.game.plant.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import model.game.Board;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A thrown explosive used to go off on the very tick it was planted, which gave the rig no chance
 * to play the explode clip it ships -- a cherry bomb was gone before one frame of it was drawn.
 * It burns for a fuse now, and the renderer runs that clip across the fuse.
 *
 * <p>What the blast itself does is unchanged, which is most of what is checked here.
 */
class ExplosiveFuseTest {

  private static PlantFactory plants;
  private static ZombieFactory zombies;

  @BeforeAll
  static void loadData() {
    new GameDataManager();
    plants = new PlantFactory(GameDataManager.plantRepository);
    zombies = new ZombieFactory(GameDataManager.zombieRepository);
  }

  private static Board lawn() {
    Board board = new Board(5, 9);
    board.getGameState().setSkySunDisabled(true);
    return board;
  }

  private static ExplodeAction fuseOf(Plant plant) {
    return plant.getBehavior() instanceof ExplodeAction explode ? explode : null;
  }

  @Test
  void aCherryBombBurnsBeforeItGoesOff() {
    Board board = lawn();
    Plant cherry = plants.createPlant("Cherry Bomb", 2, 3);
    board.placePlant(cherry);
    ExplodeAction fuse = fuseOf(cherry);
    assertTrue(fuse != null && fuse.getFuseTicks() > 0, "it needs a fuse to animate across");

    int blastTick = -1;
    for (int tick = 0; tick < 30 && blastTick < 0; tick++) {
      board.updateAll(tick);
      if (cherry.isDead()) {
        blastTick = tick;
      }
    }

    assertEquals(fuse.getFuseTicks(), blastTick,
        "it has to go off on the tick the fuse runs out, not before and not after");
  }

  @Test
  void theFuseRunsCleanlyFromNothingToAllOfIt() {
    Board board = lawn();
    Plant cherry = plants.createPlant("Cherry Bomb", 2, 3);
    board.placePlant(cherry);
    ExplodeAction fuse = fuseOf(cherry);

    board.updateAll(0);
    assertEquals(0.0, fuse.fuseProgress(cherry, 0), 1e-9, "nothing has burnt down on tick one");

    double last = 0.0;
    for (int tick = 1; tick < fuse.getFuseTicks(); tick++) {
      board.updateAll(tick);
      double burnt = fuse.fuseProgress(cherry, tick);
      assertTrue(burnt > last, "the fuse has to creep forward every tick");
      assertTrue(burnt <= 1.0, "and never overshoot the blast");
      last = burnt;
    }
    assertTrue(last > 0.8, "by the last tick before the blast it is nearly through, was " + last);
  }

  @Test
  void theBlastItselfIsUnchanged() {
    Board board = lawn();
    Plant cherry = plants.createPlant("Cherry Bomb", 2, 3);
    board.placePlant(cherry);
    Zombie inRange = zombies.createZombie("ZombieMummyDefault", 2, 3.9);
    Zombie outOfRange = zombies.createZombie("ZombieMummyDefault", 2, 7.0);
    board.spawnZombie(inRange);
    board.spawnZombie(outOfRange);
    int far = outOfRange.getCurrentHealth();

    for (int tick = 0; tick <= fuseOf(cherry).getFuseTicks(); tick++) {
      board.updateAll(tick);
    }

    assertTrue(inRange.isDead(), "a zombie beside it still dies");
    assertEquals(far, outOfRange.getCurrentHealth(),
        "and one four tiles away is still well outside the blast");
  }

  /**
   * These plants have a single hit point, so a zombie already on the tile can bite one apart
   * during the fuse. Before there was a fuse that could not happen; now it can, and the blast must
   * not simply be lost when it does.
   */
  @Test
  void oneBittenApartMidFuseStillGoesOff() {
    Board board = lawn();
    Plant cherry = plants.createPlant("Cherry Bomb", 2, 3);
    board.placePlant(cherry);
    Zombie biter = zombies.createZombie("ZombieMummyDefault", 2, 3.0);
    board.spawnZombie(biter);

    // Standing right on it, the zombie bites through its one hit point long before the fuse ends.
    board.updateAll(0);

    assertTrue(cherry.isDead(), "test setup: it was eaten rather than burning down");
    assertTrue(biter.isDead(),
        "the bomb it was eating still went off in its face, so it does not walk away with it");
  }

  @Test
  void aTrapStillWaitsToBeSteppedOnRatherThanTicking() {
    Board board = lawn();
    Plant mine = plants.createPlant("Potato Mine", 2, 3);
    board.placePlant(mine);
    ExplodeAction fuse = fuseOf(mine);

    for (int tick = 0; tick < 200; tick++) {
      board.updateAll(tick);
    }

    assertFalse(mine.isDead(), "with nothing to step on it, a mine just sits there armed");
    assertTrue(fuse.isArmed(), "and it should have finished arming");
    assertEquals(-1, fuse.fuseProgress(mine, 200), 1e-9,
        "a trap has no burning fuse for the renderer to animate");
  }
}
