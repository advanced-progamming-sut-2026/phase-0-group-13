package model.environment.greenhouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.Result;
import model.account.User;

public class GreenHouse {
  private final int maxCapacity;
  private final List<Pot> pots;

  public GreenHouse() {
    this.maxCapacity = 20;
    this.pots = new ArrayList<>(maxCapacity);

    for (int i = 0; i < maxCapacity; i++) {
      pots.add(new Pot(i < 5));
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

  public Pot getPotAt(int index) {
    if (index < 0 || index >= pots.size()) return null;
    return pots.get(index);
  }

  public Result plantSeed(int index, User user) {
    Pot pot = getPotAt(index);
    if (pot == null) return new Result(false, "error: pot index out of range", null);
    if (!pot.isUnlocked()) return new Result(false, "error: that pot is still locked", null);
    if (!pot.isEmpty())
      return new Result(false, "error: that pot already has a seed growing", null);

    String seedToPlant;
    long duration;

    if (Math.random() < 0.5) {
      seedToPlant = "marigold";
      duration = 2L * 60 * 60 * 1000;
    } else {
      List<String> unlocked = user.getUnlockedPlants();
      if (unlocked.isEmpty()) {
        seedToPlant = "marigold";
        duration = 2L * 60 * 60 * 1000;
      } else {
        seedToPlant = unlocked.get(new Random().nextInt(unlocked.size()));
        duration = 8L * 60 * 60 * 1000;
      }
    }

    pot.plant(seedToPlant, duration);
    return new Result(true, "Planted a " + seedToPlant + "! It will be ready in due time.", pot);
  }

  public Result collectSeed(int index) {
    Pot pot = getPotAt(index);
    if (pot == null) return new Result(false, "error: pot index out of range", null);
    if (pot.isEmpty()) return new Result(false, "error: that pot is empty", null);

    if (!pot.isFullyGrown()) {
      long remainingMinutes = (pot.getRemainingGrowTime() / 1000) / 60;
      return new Result(
              false, "error: seed is still growing, ~" + remainingMinutes + " minute(s) left", null);
    }

    String seedId = pot.getPlantedSeedId();
    pot.collect();
    return new Result(true, "Collected plant!", seedId);
  }

  public Result forceGrow(int index) {
    Pot pot = getPotAt(index);
    if (pot == null) return new Result(false, "error: pot index out of range", null);
    if (pot.isEmpty()) return new Result(false, "error: that pot is empty", null);
    if (pot.isFullyGrown())
      return new Result(false, "error: that seed is already fully grown", null);

    pot.forceFinishGrowth();
    return new Result(true, "The plant grew instantly!", pot);
  }
}