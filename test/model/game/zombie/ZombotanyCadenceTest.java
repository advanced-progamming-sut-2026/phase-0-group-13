package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.Board;
import model.game.zombie.behavior.ZombotanyPeashooterAction;
import model.game.zombie.factory.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The cadence a Zombotany's head is animated against.
 *
 * <p>Its shooting lives on the plant head bolted to its shoulders, and the head is drawn playing
 * its plant's attack clip across the ticks leading up to the pea so the clip finishes on the tick
 * the shot is created. That only lines up while the interval the view reads is the one the
 * zombie really fires on.
 */
class ZombotanyCadenceTest {

  private static ZombieFactory zombies;

  @BeforeAll
  static void loadTheGameData() {
    new GameDataManager();
    zombies = new ZombieFactory(GameDataManager.zombieRepository);
  }

  private static Zombie zombotany() {
    Zombie zombie = zombies.createZombie("ZombieZombotanyPeashooterDefault", 2, 7.0);
    assertNotNull(zombie, "the Zombotany Peashooter did not build");
    return zombie;
  }

  @Test
  void itDeclaresTheIntervalTheViewWindsItsHeadUpOn() {
    Zombie zombie = zombotany();
    assertTrue(zombie.getBehavior() instanceof ZombotanyPeashooterAction,
        "the view schedules the head's attack clip off this behaviour");
    ZombotanyPeashooterAction shooter = (ZombotanyPeashooterAction) zombie.getBehavior();
    assertTrue(shooter.getShootInterval() > 0,
        "with no interval there is nothing for the wind-up to finish on");
  }

  @Test
  void itFiresOnTheIntervalItDeclares() {
    Board board = new Board(5, 9);
    Zombie zombie = zombotany();
    board.spawnZombie(zombie);
    ZombotanyPeashooterAction shooter = (ZombotanyPeashooterAction) zombie.getBehavior();
    int interval = shooter.getShootInterval();

    List<Integer> fired = new ArrayList<>();
    for (int tick = 1; tick <= interval * 5; tick++) {
      int before = shooter.getLastShootTick();
      board.updateAll(tick);
      if (shooter.getLastShootTick() == tick && before != tick) {
        fired.add(tick);
      }
    }
    assertTrue(fired.size() > 2, "the Zombotany barely fired: " + fired);
    for (int i = 1; i < fired.size(); i++) {
      assertEquals(interval, fired.get(i) - fired.get(i - 1),
          "its shots drifted off the interval, so the head's wind-up would play against nothing");
    }
  }

  @Test
  void theTickItLastFiredOnIsTheTickThePeaWasCreated() {
    // The wind-up is measured back from lastShootTick + interval, so the two must agree: the view
    // holds the last frame of the clip on the tick the shot exists and no other.
    Board board = new Board(5, 9);
    Zombie zombie = zombotany();
    board.spawnZombie(zombie);
    ZombotanyPeashooterAction shooter = (ZombotanyPeashooterAction) zombie.getBehavior();

    int shots = 0;
    for (int tick = 1; tick <= shooter.getShootInterval() * 3; tick++) {
      int before = board.getProjectiles().size();
      board.updateAll(tick);
      if (board.getProjectiles().size() > before) {
        assertEquals(tick, shooter.getLastShootTick(),
            "a pea appeared on a tick the behaviour does not say it shot on");
        shots++;
      }
    }
    assertTrue(shots > 0, "the Zombotany never fired at all");
  }
}
