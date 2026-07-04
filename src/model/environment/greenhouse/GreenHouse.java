package model.environment.greenhouse;

import java.util.ArrayList;
import java.util.List;

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
  
}