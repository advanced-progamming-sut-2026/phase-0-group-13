package model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SunManager {
  private final List<Sun> suns;
  private int lastSunDropTick;
  private final Random random;

  public SunManager() {
    this.suns = new ArrayList<>();
    this.lastSunDropTick = 0;
    this.random = new Random();
  }

  public void update(int currentTick, Board board) {
    handleSkySunDrop(currentTick, board);

    for (Sun sun : suns) {
      sun.update(currentTick);
    }
  }

  public void handleSkySunDrop(int currentTick, Board board) {
    if (board.getGameState().isSkySunDisabled()) {
      return;
    }

    double t = currentTick / 10.0;
    double secondsInterval = Math.max(6 + 0.05 * t, 12);
    int ticksInterval = (int) (secondsInterval * 10);

    if (currentTick - lastSunDropTick >= ticksInterval) {
      lastSunDropTick = currentTick;

      int targetRow = random.nextInt(board.getRows());
      int targetCol = random.nextInt(board.getColumns());

      int roll = random.nextInt(100);
      model.enums.SunType type;
      int amount;

      if (roll < 80) {
        type = model.enums.SunType.NORMAL;
        amount = 25;
      } else if (roll < 95) {
        type = model.enums.SunType.LARGE;
        amount = 100;
      } else {
        type = model.enums.SunType.RADIOACTIVE;
        amount = 25;
      }

      Sun newSun = new Sun(amount, 150, type, true);
      newSun.changinCordinate(targetCol, targetRow);
      suns.add(newSun);

      System.out.printf(
          "New %s sun is dropping at position (%d, %d)%n",
          type.name().toLowerCase(), targetCol + 1, targetRow + 1);
    }
  }

  public Integer collectSunAt(int col, int row, Board board) {
    for (Sun sun : suns) {
      if (!sun.isExpired() && Math.abs(sun.getX() - col) <= 0.5 && sun.getY() == row) {
        if (sun.getType() == model.enums.SunType.RADIOACTIVE && sun.isFalling()) {
          System.out.printf("Radioactive sun exploded at (%d, %d)!%n", col + 1, row + 1);
          board.applyAreaDamageToZombies(col, row, 2, 150);
          board.applyAreaDamageToPlants(col, row, 1, 80);
          sun.setCollected(true);
          return 0;
        }
        int amount = sun.getAmount();
        sun.collect(board.getGameState());
        return amount;
      }
    }
    return null;
  }

  public void addSun(Sun s) {
    suns.add(s);
  }

  public List<Sun> getSuns() {
    return suns;
  }

  public void cleanupExpiredSuns() {
    suns.removeIf(Sun::isExpired);
  }
}