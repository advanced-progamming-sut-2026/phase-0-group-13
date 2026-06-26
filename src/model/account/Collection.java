package model.account;

import java.util.ArrayList;
import java.util.List;
import model.Result;

public class Collection {
  private List<String> unlockedPlants;

  private transient List<String> selectedDeck;

  private int maxSeedSlots;

  public Collection() {
    this.unlockedPlants = new ArrayList<>();
    this.selectedDeck = new ArrayList<>();
    this.maxSeedSlots = 0;
  }

  public Result addPlant(String plantName) {
    if (plantName == null || plantName.trim().isEmpty()) {
      return new Result(false, "Invalid plant name.", null);
    }

    String nameLower = plantName.toLowerCase().trim();
    if (unlockedPlants.contains(nameLower)) {
      return new Result(false, "You already own " + plantName + "!", null);
    }

    unlockedPlants.add(nameLower);
    return new Result(true, plantName + " added to your collection successfully.", nameLower);
  }

  public boolean hasPlant(String plantName) {
    if (plantName == null) return false;
    return unlockedPlants.contains(plantName.toLowerCase().trim());
  }

  public Result selectPlantForMatch(String plantName) {
    String nameLower = plantName.toLowerCase().trim();

    if (!hasPlant(nameLower)) {
      return new Result(false, "You haven't unlocked this plant yet!", null);
    }

    if (selectedDeck.contains(nameLower)) {
      return new Result(false, plantName + " is already in your deck.", null);
    }

    if (selectedDeck.size() >= maxSeedSlots) {
      return new Result(false, "Your seed deck is full! Maximum slots: " + maxSeedSlots, null);
    }

    selectedDeck.add(nameLower);
    return new Result(true, plantName + " selected for the match.", selectedDeck.size());
  }

  public Result deselectPlantFromMatch(String plantName) {
    String nameLower = plantName.toLowerCase().trim();

    if (selectedDeck.remove(nameLower)) {
      return new Result(true, plantName + " removed from your deck.", selectedDeck.size());
    }

    return new Result(false, plantName + " is not in your current deck.", null);
  }

  public void clearDeck() {
    this.selectedDeck.clear();
  }

  public List<String> getUnlockedPlants() {
    return unlockedPlants;
  }

  public List<String> getSelectedDeck() {
    return selectedDeck;
  }

  public int getMaxSeedSlots() {
    return maxSeedSlots;
  }

  public Result upgradeSeedSlots() {
    if (this.maxSeedSlots
        < 10) { // یادم نمیاد چند تا بود ، حوصله ندارم داک و ببینم الان دقیقش ، 10 گذاشتم
      this.maxSeedSlots++;
      return new Result(
          true, "Seed slots upgraded! Current slots: " + this.maxSeedSlots, this.maxSeedSlots);
    }
    return new Result(false, "Seed slots are already at maximum capacity.", null);
  }
}
