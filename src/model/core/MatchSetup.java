package model.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchSetup {

  private static MatchSetup instance;

  private String targetChapter;
  private List<String> selectedPlants;
  private List<String> boostedPlants;

  private MatchSetup() {
    this.selectedPlants = new ArrayList<>();
    this.boostedPlants = new ArrayList<>();
  }

  public static MatchSetup getInstance() {
    if (instance == null) {
      instance = new MatchSetup();
    }
    return instance;
  }

  public void setTargetChapter(String chapter) {
    this.targetChapter = chapter;
  }

  public String getTargetChapter() {
    return targetChapter;
  }

  public void setSelectedPlants(List<String> plants) {
    this.selectedPlants = plants == null ? new ArrayList<>() : new ArrayList<>(plants);
  }

  public List<String> getSelectedPlants() {
    return Collections.unmodifiableList(selectedPlants);
  }

  public void setBoostedPlants(List<String> plants) {
    this.boostedPlants = plants == null ? new ArrayList<>() : new ArrayList<>(plants);
  }

  public List<String> getBoostedPlants() {
    return Collections.unmodifiableList(boostedPlants);
  }

  public void clear() {
    this.targetChapter = null;
    this.selectedPlants.clear();
    this.boostedPlants.clear();
  }
}
