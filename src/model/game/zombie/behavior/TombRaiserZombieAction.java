package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.game.Board;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class TombRaiserZombieAction implements ZombieAction {
  private static final int TOMBSTONE_HEALTH = 400;

  /**
   * A raised grave stops direct shots, the same as every other grave.
   *
   * <p>The doc describes one kind of grave and gives it one rule: it "prevents the passage of
   * shots from plants that shoot horizontally", it takes damage when a shot hits it, and only once
   * it is destroyed can direct shots pass through that tile. It says nothing about the Tomb
   * Raiser's graves being different -- they are simply graves, thrown onto two tiles.
   *
   * <p>This was false, which made the Tomb Raiser's graves scenery: {@code
   * Board.isBlockedByTombstone} skips a grave that does not block, so shots flew straight through
   * to the zombie behind, and the grave itself never took a point of damage and so could never be
   * destroyed. The graves the seasons place were already true; only this one disagreed.
   */
  private static final boolean TOMBSTONE_BLOCKS_SHOTS = true;

  private final int raiseInterval;
  private final double eatingDamage;
  private int lastRaiseTick = -1;
  private final Random random = new Random();


  public TombRaiserZombieAction(int raiseInterval, double eatingDamage) {
    this.raiseInterval = raiseInterval;
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (lastRaiseTick == -1) {
      lastRaiseTick = currentTick;
    }
    if (currentTick - lastRaiseTick >= raiseInterval) {
      spawnTombstoneOnRandomEmptyTile(board);
      lastRaiseTick = currentTick;
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }

  private void spawnTombstoneOnRandomEmptyTile(Board board) {
    List<int[]> emptyTiles = new ArrayList<>();
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        boolean tileHasPlant = board.getPlantAt(row, col) != null;
        boolean tileHasEffect =
                board.getTile(row, col) != null && board.getTile(row, col).getEffect() != null;
        if (!tileHasPlant && !tileHasEffect) {
          emptyTiles.add(new int[] {row, col});
        }
      }
    }
    if (emptyTiles.isEmpty()) {
      return;
    }
    int[] chosen = emptyTiles.get(random.nextInt(emptyTiles.size()));
    board.placeTileEffect(
            chosen[0], chosen[1], new TombStoneEffect(TOMBSTONE_HEALTH, TOMBSTONE_BLOCKS_SHOTS));
    System.out.printf(
            "A tombstone rises from the ground at (%d, %d)!%n", chosen[1] + 1, chosen[0] + 1);
  }
}