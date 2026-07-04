package model.environment.greenhouse;

import model.account.User;

public class PlantUpgrade {
  private String fromPlant;
  private String toPlant;
  private int requiredSun;

  public PlantUpgrade() {}

  public PlantUpgrade(String fromPlant, String toPlant, int requiredSun) {
    this.fromPlant = fromPlant;
    this.toPlant = toPlant;
    this.requiredSun = requiredSun;
  }

  public String getFromPlant() { return fromPlant; }
  public String getToPlant() { return toPlant; }
  public int getRequiredSun() { return requiredSun; }

  public boolean applyUpgrade(User user, int availableSun) {
    if (availableSun < this.requiredSun) {
      return false;
    }

    if (!user.getUnlockedPlants().contains(this.fromPlant.toLowerCase())) {
      return false;
    }

    user.unlockItem("plant_" + this.toPlant.toLowerCase());

    return true;
  }
}