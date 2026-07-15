package model.game.zombie.ZombieParts;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class ZombieTemplate {

  @SerializedName("aliases")
  public List<String> aliases;

  @SerializedName("objclass")
  public String objclass;

  @SerializedName("objdata")
  public ObjData objdata;

  public static class ObjData {
    @SerializedName("Hitpoints")
    public Integer hitpoints;

    @SerializedName("Speed")
    public Double speed;

    @SerializedName("Cost")
    public Integer cost;

    @SerializedName("EatDPS")
    public Double eatDps;

    @SerializedName("WavePointCost")
    public Integer wavePointCost;

    @SerializedName("Weight")
    public Integer weight;

    @SerializedName("ZombieArmorProps")
    public List<String> zombieArmorProps;

    @SerializedName("ZombieStats")
    public List<ZombieStatEntry> zombieStats;

    @SerializedName("ZombieTypesToSpawn")
    public List<WeightedSpawn> zombieTypesToSpawn;

    @SerializedName("ArmorType")
    public String armorType;

    @SerializedName("BaseHealth")
    public Integer armorBaseHealth;

    @SerializedName("ArmorLayers")
    public List<String> armorLayers;

    @SerializedName("ArmorLayerHealth")
    public List<Double> armorLayerHealth;

    @SerializedName("ArmorFlags")
    public List<String> armorFlags;

    @SerializedName("FireLayer")
    public String fireLayer;
  }

  public static class ZombieStatEntry {
    @SerializedName("Type")
    public String type;

    @SerializedName("Value")
    public String value;
  }

  public static class WeightedSpawn {
    @SerializedName("Weight")
    public int weight;

    @SerializedName("ZombieTypeName")
    public String zombieTypeName;
  }


  public boolean isArmorDefinition() {
    return "ArmorPropertySheet".equals(objclass) || "NewspaperArmorPropertySheet".equals(objclass);
  }

  public String getName() {
    return (aliases != null && !aliases.isEmpty()) ? aliases.get(0) : null;
  }

  public int getBaseHp() {
    return (objdata != null && objdata.hitpoints != null) ? objdata.hitpoints : 0;
  }

  public double getBaseSpeed() {
    return (objdata != null && objdata.speed != null) ? objdata.speed : 0.0;
  }

  public double getEatDps() {
    return (objdata != null && objdata.eatDps != null) ? objdata.eatDps : 10.0;
  }

  public List<String> getArmorRefAliases() {
    if (objdata == null || objdata.zombieArmorProps == null) {
      return List.of();
    }
    return objdata.zombieArmorProps.stream()
            .map(s -> s.replaceAll("RTID\\((.*?)@ArmorTypes\\)", "$1"))
            .toList();
  }


  public String getStatsSummary() {
    StringBuilder sb = new StringBuilder();

    if (objdata != null && objdata.zombieStats != null && !objdata.zombieStats.isEmpty()) {
      for (ZombieStatEntry stat : objdata.zombieStats) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(stat.type).append("=").append(stat.value);
      }
    }

    List<String> armor = getArmorRefAliases();
    if (!armor.isEmpty()) {
      if (sb.length() > 0) sb.append("; ");
      sb.append("armor: ").append(String.join(", ", armor));
    }

    return sb.length() > 0 ? sb.toString() : "none";
  }
}