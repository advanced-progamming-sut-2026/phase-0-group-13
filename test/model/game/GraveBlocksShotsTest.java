package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.game.TileEffects.TombStoneEffect;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import model.game.zombie.behavior.TombRaiserZombieAction;
import org.junit.jupiter.api.Test;

/**
 * The doc gives graves one rule: they "prevent the passage of shots from plants that shoot
 * horizontally", they have health and "take damage when a shot hits", and only "when its health
 * is zero" does the tile go back to ordinary ground. So a zombie standing behind a grave is safe
 * from direct fire until the grave is gone, and every shot spent on it has to actually count.
 *
 * <p>The Tomb Raiser builds graves too -- the doc describes it throwing gravestones onto two tiles
 * so that "in those two houses a grave comes into existence", with no suggestion they are a
 * different kind of object. Its graves used to be built non-blocking, which made
 * {@link Board#handleProjectiles} skip them entirely: shots flew through to the zombie and the
 * grave never took a point of damage, so it could never be destroyed either.
 */
class GraveBlocksShotsTest {

  private static final int ROW = 2;
  private static final int GRAVE_COL = 4;
  private static final int DAMAGE = 20;

  /** A zombie parked behind the grave, further from the house than the grave is. */
  private static Zombie zombieBehindTheGrave(Board board) {
    Zombie zombie = new Zombie("basic", 500, 0.0, ROW, 6.0, new StandardZombieAction(10));
    board.spawnZombie(zombie);
    return zombie;
  }

  /** Fires from column 1 the way a peashooter does, and runs the board on. */
  private static void fireAt(Board board, int shots) {
    int tick = 0;
    for (int i = 0; i < shots; i++) {
      board.addProjectile(new Projectile(DAMAGE, 0.5, 1, ROW, false, false));
      for (int step = 0; step < 15; step++) {
        board.updateAll(tick++);
      }
    }
  }

  @Test
  void aGraveTakesTheShotAndTheZombieBehindItIsUntouched() {
    Board board = new Board(5, 9);
    TombStoneEffect grave = new TombStoneEffect(700, true);
    board.placeTileEffect(ROW, GRAVE_COL, grave);
    Zombie zombie = zombieBehindTheGrave(board);

    fireAt(board, 5);

    assertTrue(grave.isActive(), "five shots should not have finished a 700hp grave");
    assertTrue(grave.getHealth() < grave.getMaxHealth(), "the grave took no damage at all");
    assertEquals(500, zombie.getCurrentHealth(),
        "a shot reached the zombie through a standing grave");
  }

  @Test
  void enoughShotsDestroyTheGraveAndThenTheZombieIsHit() {
    Board board = new Board(5, 9);
    TombStoneEffect grave = new TombStoneEffect(60, true);
    board.placeTileEffect(ROW, GRAVE_COL, grave);
    Zombie zombie = zombieBehindTheGrave(board);

    // exactly enough to break a 60hp grave at 20 a shot
    fireAt(board, 3);
    assertFalse(grave.isActive(), "60hp of grave should not survive 60 damage");
    assertEquals(500, zombie.getCurrentHealth(), "the grave should have absorbed all three");

    // and now the path is open
    fireAt(board, 1);
    assertTrue(zombie.getCurrentHealth() < 500,
        "with the grave destroyed the next shot should reach the zombie");
  }

  @Test
  void aBrokenGraveStopsBlockingAndStopsBlockingPlanting() {
    TombStoneEffect grave = new TombStoneEffect(40, true);
    assertTrue(grave.blocksPlanting());
    grave.takeDamage(40);
    assertFalse(grave.isActive());
    assertFalse(grave.isBlocksShots(), "a destroyed grave must not keep eating shots");
    assertFalse(grave.blocksPlanting(), "the tile goes back to ordinary ground");
  }

  @Test
  void theTombRaiserRaisesGravesThatBlockLikeEveryOtherGrave() {
    Board board = new Board(5, 9);
    Zombie raiser = new Zombie("ZombieTombRaiserDefault", 300, 0.0, ROW, 8.0,
        new TombRaiserZombieAction(1, 10));
    board.spawnZombie(raiser);

    // Two ticks past the raise interval is enough for it to throw its stones.
    for (int tick = 0; tick < 4; tick++) {
      board.updateAll(tick);
    }

    TombStoneEffect raised = findGrave(board);
    assertTrue(raised != null, "the Tomb Raiser did not raise a grave");
    assertTrue(raised.isBlocksShots(),
        "a Tomb Raiser's grave is still a grave, so it has to stop direct shots");

    int before = raised.getHealth();
    raised.takeDamage(DAMAGE);
    assertEquals(before - DAMAGE, raised.getHealth(), "a raised grave must be damageable");
  }

  private static TombStoneEffect findGrave(Board board) {
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        if (board.getTile(row, col) != null
            && board.getTile(row, col).getEffect() instanceof TombStoneEffect grave
            && grave.isActive()) {
          return grave;
        }
      }
    }
    return null;
  }
}
