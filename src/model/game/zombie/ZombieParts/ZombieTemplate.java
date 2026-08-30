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

    @SerializedName("Stages")
    public List<Stage> stages;
  }

  /** یک فاز از باس؛ زامبوس‌ها به جای Hitpoints، جانشان را در Stages ثبت کرده‌اند. */
  public static class Stage {
    @SerializedName("HitPoints")
    public Integer hitPoints;
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

  /** The player-facing name. getName() stays the raw alias because art and saves key off it. */
  public String getDisplayName() {
    String alias = getName();
    if (alias == null) {
      return null;
    }
    return ZombieTypeResolver.resolve(this).getDisplayName();
  }

  public int getBaseHp() {
    if (objdata != null && objdata.hitpoints != null) {
      return objdata.hitpoints;
    }
    return getStagedHp();
  }

  /**
   * جان باس‌ها. زامبوس‌ها فیلد Hitpoints ندارند و جانشان در Stages[].HitPoints (سه فاز) ثبت شده،
   * پس جان کلشان جمع فازهاست؛ بدون این، getBaseHp صفر برمی‌گرداند و فکتوری مجبور می‌شود یک عدد
   * ثابت جایگزین کند.
   */
  public int getStagedHp() {
    if (objdata == null || objdata.stages == null) {
      return 0;
    }
    int total = 0;
    for (Stage stage : objdata.stages) {
      if (stage != null && stage.hitPoints != null) {
        total += stage.hitPoints;
      }
    }
    return total;
  }

  /** جان هر فاز، به ترتیب. برای زامبی‌های عادی خالی است. */
  public List<Integer> getStageHitPoints() {
    if (objdata == null || objdata.stages == null) {
      return List.of();
    }
    List<Integer> points = new java.util.ArrayList<>();
    for (Stage stage : objdata.stages) {
      if (stage != null && stage.hitPoints != null) {
        points.add(stage.hitPoints);
      }
    }
    return points;
  }

  public double getBaseSpeed() {
    return (objdata != null && objdata.speed != null) ? objdata.speed : 0.0;
  }

  public double getEatDps() {
    return (objdata != null && objdata.eatDps != null) ? objdata.eatDps : 10.0;
  }

  public int getWavePointCost() {
    return (objdata != null && objdata.wavePointCost != null) ? objdata.wavePointCost : 0;
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