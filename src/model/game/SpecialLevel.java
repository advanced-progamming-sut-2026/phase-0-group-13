package model.game;

import java.util.List;

public class SpecialLevel {
  private LevelType levelType;
  private List<String> plantList;
  private List<Wave> attackPatterns;

  public enum LevelType {
    NIGHT_OPS,
    DEAD_LINE,
    LOVE_YOUR_PLANTS
  }

  public SpecialLevel(LevelType levelType, List<String> plantList, List<Wave> attackPatterns) {
    this.levelType = levelType;
    this.plantList = plantList;
    this.attackPatterns = attackPatterns;
  }

  public LevelType getLevelType() {
    return levelType;
  }

  public void setLevelType(LevelType levelType) {
    this.levelType = levelType;
  }

  public List<String> getPlantList() {
    return plantList;
  }

  public void setPlantList(List<String> plantList) {
    this.plantList = plantList;
  }

  public List<Wave> getAttackPatterns() {
    return attackPatterns;
  }

  public void setAttackPatterns(List<Wave> attackPatterns) {
    this.attackPatterns = attackPatterns;
  }
}
