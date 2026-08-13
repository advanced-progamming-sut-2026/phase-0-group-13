package model.game.plant.behavior;

import java.util.Random;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * اثر گیاهان تگ moveZombies (مثل Garlic و Sweet Potato): زامبی‌ای که به گیاه می‌رسد به جای
 * خوردن آن، به یکی از ردیف‌های همسایه هدایت می‌شود.
 */
public class LaneRedirectAction implements PlantAction {
  private static final double REACH = 0.6;

  private final Random random = new Random();
  private final int bitesBeforeDying;
  private final boolean laneWide;
  private int redirects;

  public LaneRedirectAction(int bitesBeforeDying) {
    this(bitesBeforeDying, false);
  }

  /**
   * laneWide برای اثر غذای گیاهِ Garlic است: دیتا می‌گوید «هر زامبیِ داخل ردیف» را بیرون می‌کند،
   * نه فقط آن یکی که چسبیده به گیاه.
   */
  public LaneRedirectAction(int bitesBeforeDying, boolean laneWide) {
    this.bitesBeforeDying = Math.max(1, bitesBeforeDying);
    this.laneWide = laneWide;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (board.getRows() < 2) {
      return;
    }

    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()
              || zombie.isHypnotized()
              || zombie.getRow() != plant.getRow()
              || (!laneWide && Math.abs(zombie.getX() - plant.getCol()) > REACH)) {
        continue;
      }

      int targetRow = pickNeighbourRow(board, plant.getRow());
      zombie.setRow(targetRow);
      zombie.setEating(false);
      redirects++;
      System.out.printf("%s pushed %s from row %d to row %d!%n",
              plant.getName(), zombie.getName(), plant.getRow() + 1, targetRow + 1);

      if (redirects >= bitesBeforeDying) {
        plant.takeDamage(plant.getMaxHealth());
        System.out.printf("%s is used up.%n", plant.getName());
        return;
      }
    }
  }

  private int pickNeighbourRow(Board board, int row) {
    if (row == 0) {
      return 1;
    }
    if (row == board.getRows() - 1) {
      return row - 1;
    }
    return random.nextBoolean() ? row - 1 : row + 1;
  }
}
