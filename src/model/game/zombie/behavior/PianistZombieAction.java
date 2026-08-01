package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

// زامبی پیانیست آروم جلو میاد و پیانوش رو از روی گیاه‌های سر راه رد میکنه (واینمیسته بخوره)، و هر
// چند ثانیه یه بار با نواختن پیانو، زامبی‌های یه ردیف تصادفی رو به یکی از ردیف‌های همسایه میبره
public class PianistZombieAction implements ZombieAction {
  private final int tuneIntervalTicks;
  private final Random random = new Random();
  private int lastTuneTick = -1;

  public PianistZombieAction(int tuneIntervalTicks) {
    this.tuneIntervalTicks = Math.max(1, tuneIntervalTicks);
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    playTune(zombie, board, currentTick);

    Plant crushed = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (crushed != null && !crushed.isDead()) {
      crushed.takeDamage(10000);
      System.out.printf("%s rolled its piano over %s!%n", zombie.getName(), crushed.getName());
    }

    zombie.setEating(false);
    zombie.move();
  }

  private void playTune(Zombie zombie, Board board, int currentTick) {
    if (lastTuneTick == -1) {
      lastTuneTick = currentTick;
      return;
    }
    if (currentTick - lastTuneTick < tuneIntervalTicks || board.getRows() < 2) {
      return;
    }
    lastTuneTick = currentTick;

    int sourceRow = random.nextInt(board.getRows());
    List<Zombie> dancers = new ArrayList<>();
    for (Zombie other : board.getZombies()) {
      if (!other.isDead() && other.getRow() == sourceRow) {
        dancers.add(other);
      }
    }
    if (dancers.isEmpty()) {
      return;
    }

    int targetRow = pickNeighbourRow(board, sourceRow);
    for (Zombie dancer : dancers) {
      dancer.setRow(targetRow);
    }
    System.out.printf("%s played a tune; %d zombie(s) shuffled from row %d to row %d!%n",
            zombie.getName(), dancers.size(), sourceRow + 1, targetRow + 1);
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
