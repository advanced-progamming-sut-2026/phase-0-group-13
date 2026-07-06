package model.environment.greenhouse;

import java.util.ArrayList;
import java.util.List;
import model.Result;

public class GreenHouse {
  private final int maxCapacity;
  private final List<Pot> pots;

  public GreenHouse() {
    this.maxCapacity = 20;
    this.pots = new ArrayList<>(maxCapacity);

    for (int i = 0; i < maxCapacity; i++) {
      pots.add(new Pot(false));
    }
  }

  public int getMaxCapacity() {
    return maxCapacity;
  }

  public List<Pot> getPots() {
    return pots;
  }

  public boolean unlockNextPot() {
    for (Pot pot : pots) {
      if (!pot.isUnlocked()) {
        pot.unlock();
        return true;
      }
    }
    return false;
  }

  public int getUnlockedPotsCount() {
    int count = 0;
    for (Pot pot : pots) {
      if (pot.isUnlocked()) count++;
    }
    return count;
  }

  private Pot getPotAt(int index) {
    if (index < 0 || index >= pots.size()) return null;
    return pots.get(index);
  }

  public Result plantSeed(int index, String seedId) {
    Pot pot = getPotAt(index);
    if (pot == null) {
      return new Result(false, "error: pot index out of range", null);
    }
    if (!pot.isUnlocked()) {
      return new Result(false, "error: that pot is still locked", null);
    }
    if (!pot.isEmpty()) {
      return new Result(false, "error: that pot already has a seed growing", null);
    }
    pot.plant(seedId);
    return new Result(true, "Seed planted! It will be ready in due time.", pot);
  }

  public Result collectSeed(int index) {
    Pot pot = getPotAt(index);
    if (pot == null) {
      return new Result(false, "error: pot index out of range", null);
    }
    if (pot.isEmpty()) {
      return new Result(false, "error: that pot is empty", null);
    }
    if (!pot.isFullyGrown()) {
      long remainingMs = pot.getRemainingGrowTime();
      long remainingMinutes = (remainingMs / 1000) / 60;
      return new Result(
          false, "error: seed is still growing, ~" + remainingMinutes + " minute(s) left", null);
    }

    String seedId = pot.getPlantedSeedId();
    pot.collect();
    return new Result(true, "Collected seed: " + seedId, seedId);
  }

  public Result forceGrow(int index) {
    Pot pot = getPotAt(index);
    if (pot == null) {
      return new Result(false, "error: pot index out of range", null);
    }
    if (pot.isEmpty()) {
      return new Result(false, "error: that pot is empty", null);
    }
    if (pot.isFullyGrown()) {
      return new Result(false, "error: that seed is already fully grown", null);
    }
    pot.forceFinishGrowth();
    return new Result(true, "The seed grew instantly!", pot);
  }
}
