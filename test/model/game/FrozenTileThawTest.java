package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.enums.StatusEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.zombie.Zombie;
import model.game.zombie.behavior.StandardZombieAction;
import org.junit.jupiter.api.Test;

/**
 * Frostbite Caves lays permanent frozen tiles, and a zombie that walks onto one has to freeze,
 * thaw, and walk on.
 *
 * <p>It used to freeze and stay frozen. The tile is permanent, so {@code
 * Board.applyTileHazardsToZombies} reapplied FROZEN every tick the zombie stood on it; a frozen
 * zombie cannot move, so it could never leave the tile and the countdown was renewed faster than
 * it ran down. Observed live: a Frostbite boss stage still on wave 0 of 3 after 1,200 seconds with
 * three zombies pinned on one tile, which can neither be won nor lost because the wave gate needs
 * 75% of the wave's health destroyed.
 *
 * <p>The tile built here is exactly the one {@code FrostbiteCavesSeason.placeHazards} builds:
 * duration {@code -1} (permanent) and {@code fullFreeze} true.
 */
class FrozenTileThawTest {

  private static final int ROW = 2;
  private static final int ICE_COL = 4;
  private static final int PERMANENT = -1;

  private static Board frostbiteBoard() {
    Board board = new Board(5, 9);
    board.initialize();
    board.placeTileEffect(ROW, ICE_COL, new IceTrailEffect(PERMANENT, 0.0, true));
    return board;
  }

  private static Zombie walkerOn(Board board, double x) {
    Zombie zombie = new Zombie("basic", 500, 0.02, ROW, x, new StandardZombieAction(10));
    board.spawnZombie(zombie);
    return zombie;
  }

  private static boolean isFrozen(Zombie zombie) {
    return zombie.getActiveEffects().containsKey(StatusEffect.FROZEN);
  }

  @Test
  void theTileStillFreezesAZombieThatStepsOnIt() {
    Board board = frostbiteBoard();
    Zombie zombie = walkerOn(board, ICE_COL);

    board.updateAll(0);

    assertTrue(isFrozen(zombie), "a frozen tile must still catch a zombie standing on it");
  }

  @Test
  void aFrozenZombieDoesNotMoveWhileTheFreezeLasts() {
    Board board = frostbiteBoard();
    Zombie zombie = walkerOn(board, ICE_COL);
    board.updateAll(0);
    double held = zombie.getX();

    for (int tick = 1; tick < 20; tick++) {
      board.updateAll(tick);
    }

    assertTrue(isFrozen(zombie), "the freeze should still be running after 20 ticks");
    assertEquals(held, zombie.getX(), 1e-9, "a frozen zombie must not move");
  }

  @Test
  void theFreezeRunsDownInsteadOfBeingRenewedEveryTick() {
    Board board = frostbiteBoard();
    Zombie zombie = walkerOn(board, ICE_COL);
    board.updateAll(0);
    int first = zombie.getActiveEffects().get(StatusEffect.FROZEN);

    for (int tick = 1; tick <= 10; tick++) {
      board.updateAll(tick);
    }
    int later = zombie.getActiveEffects().get(StatusEffect.FROZEN);

    assertTrue(later < first,
        "the remaining freeze must fall; it used to be pinned by the tile every tick");
  }

  @Test
  void theZombieThawsAndLeavesTheTile() {
    Board board = frostbiteBoard();
    Zombie zombie = walkerOn(board, ICE_COL);
    double startX = zombie.getX();

    for (int tick = 0; tick < 600; tick++) {
      board.updateAll(tick);
    }

    assertFalse(isFrozen(zombie), "the zombie must thaw rather than stay frozen for ever");
    assertTrue(zombie.getX() < startX - 1.0,
        "the zombie must move off the tile; it stopped at " + zombie.getX());
  }

  @Test
  void aSecondFrozenTileFurtherDownTheRowCatchesItAgain() {
    Board board = frostbiteBoard();
    board.placeTileEffect(ROW, ICE_COL - 2, new IceTrailEffect(PERMANENT, 0.0, true));
    Zombie zombie = walkerOn(board, ICE_COL);

    boolean refrozenFurtherOn = false;
    for (int tick = 0; tick < 600 && !refrozenFurtherOn; tick++) {
      board.updateAll(tick);
      refrozenFurtherOn = isFrozen(zombie) && zombie.getX() < ICE_COL - 1.0;
    }

    assertTrue(refrozenFurtherOn, "each frozen tile must catch the zombie on its own");
  }

  @Test
  void theBossIsStillImmuneToFrozenTiles() {
    Board board = frostbiteBoard();
    Zombie boss = new Zombie("ZombieZombossMech", 9000, 0.02, ROW, ICE_COL,
        new StandardZombieAction(10));
    boss.setBoss(true);
    board.spawnZombie(boss);

    for (int tick = 0; tick < 30; tick++) {
      board.updateAll(tick);
    }

    assertFalse(isFrozen(boss), "the mech on treads must not be frozen by ground ice");
    assertEquals(Zombie.NO_CELL, boss.getIcedOnCell(), "no tile should have claimed the boss");
  }
}
