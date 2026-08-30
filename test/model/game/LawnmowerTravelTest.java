package model.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import org.junit.jupiter.api.Test;

/**
 * The mower rolls the row instead of clearing it where it stands, which is the doc's polish item.
 * What has to stay true from Phase 1 is that everything in the row still dies and that the row is
 * only protected once.
 */
class LawnmowerTravelTest {

  private static Zombie zombieAt(int row, double x) {
    return new Zombie("ZombieMummyDefault", 200, 0.01, row, x, new StandardZombieAction(10));
  }

  @Test
  void anUntriggeredMowerStaysPutAndCountsAsAvailable() {
    Lawnmower mower = new Lawnmower(0);
    double before = mower.getX();
    assertTrue(mower.isAvailable());
    mower.move(List.of(zombieAt(0, 3.0)));
    assertTrue(mower.getX() == before, "a parked mower should not move");
    assertTrue(mower.isAvailable());
  }

  @Test
  void aTriggeredMowerStopsCountingAsAvailableButIsStillOnTheLawn() {
    Lawnmower mower = new Lawnmower(0);
    mower.trigger();
    assertFalse(mower.isAvailable(), "a rolling mower no longer protects the row");
    assertTrue(mower.isActive(), "it is still on the lawn while it rolls");
  }

  @Test
  void itCrushesEveryZombieInTheRowAsItPasses() {
    Lawnmower mower = new Lawnmower(2);
    Zombie near = zombieAt(2, 1.0);
    Zombie far = zombieAt(2, 7.0);
    List<Zombie> zombies = List.of(near, far);

    mower.trigger();
    for (int tick = 0; tick < 200 && mower.isActive(); tick++) {
      mower.move(zombies);
    }

    assertTrue(near.isDead(), "the near zombie was never run over");
    assertTrue(far.isDead(), "the mower stopped before reaching the far zombie");
  }

  @Test
  void itLeavesOtherRowsAlone() {
    Lawnmower mower = new Lawnmower(1);
    Zombie otherRow = zombieAt(3, 4.0);
    List<Zombie> zombies = List.of(otherRow);

    mower.trigger();
    for (int tick = 0; tick < 200 && mower.isActive(); tick++) {
      mower.move(zombies);
    }

    assertFalse(otherRow.isDead(), "the mower crossed into another row");
  }

  @Test
  void itReportsWhatItCrushedSoTheMowingQuestCanCount() {
    Lawnmower mower = new Lawnmower(0);
    Zombie victim = zombieAt(0, 1.0);
    mower.trigger();

    boolean reported = false;
    for (int tick = 0; tick < 200 && mower.isActive(); tick++) {
      if (mower.move(List.of(victim)).contains(victim)) {
        reported = true;
      }
    }
    assertTrue(reported, "the kill was never handed back to the board");
  }

  @Test
  void itLeavesTheLawnAndGoesInactive() {
    Lawnmower mower = new Lawnmower(0);
    mower.trigger();
    for (int tick = 0; tick < 500 && mower.isActive(); tick++) {
      mower.move(List.of());
    }
    assertFalse(mower.isActive(), "the mower never drove off the lawn");
  }
}
