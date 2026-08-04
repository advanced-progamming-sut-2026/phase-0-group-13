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

  // طبق داک: «زامبی‌ها هر چند ثانیه یک‌بار به‌صورت رندوم ردیف خود را با یکی از ردیف‌های همسایه
  // جابه‌جا می‌کنند»؛ یعنی همهٔ زامبی‌های زمین (به‌جز خود پیانیست که مشغول نواختن است) هرکدام به یکی
  // از ردیف‌های همسایهٔ خودشان می‌روند، نه فقط زامبی‌های یک ردیفِ تصادفی
  private void playTune(Zombie zombie, Board board, int currentTick) {
    if (lastTuneTick == -1) {
      lastTuneTick = currentTick;
      return;
    }
    if (currentTick - lastTuneTick < tuneIntervalTicks || board.getRows() < 2) {
      return;
    }
    lastTuneTick = currentTick;

    List<Zombie> dancers = new ArrayList<>();
    for (Zombie other : board.getZombies()) {
      if (other != zombie && !other.isDead()) {
        dancers.add(other);
      }
    }
    if (dancers.isEmpty()) {
      return;
    }

    for (Zombie dancer : dancers) {
      dancer.setRow(pickNeighbourRow(board, dancer.getRow()));
    }
    System.out.printf("%s played a tune; %d zombie(s) danced into a neighbouring row!%n",
            zombie.getName(), dancers.size());
  }

  @Override
  public String debugState(Zombie zombie, int currentTick) {
    if (lastTuneTick == -1) {
      return "piano warming up";
    }
    int remaining = Math.max(0, tuneIntervalTicks - (currentTick - lastTuneTick));
    return String.format("next tune in %.1fs", remaining / 10.0);
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
