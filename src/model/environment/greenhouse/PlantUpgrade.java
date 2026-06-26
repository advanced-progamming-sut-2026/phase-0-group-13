package model.environment.greenhouse;

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

  public void applyUpgrade() {}
}
