package model.game.zombie.ZombieParts;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Mirrors ONE raw entry from Zombies.json.
 *
 * IMPORTANT — this is NOT a flat table like plants.json. Two different kinds
 * of objects share this file, distinguished by "objclass":
 *   - "ZombiePropertySheet"                -> a real zombie
 *   - "ArmorPropertySheet" / "NewspaperArmorPropertySheet" -> an armor definition
 *     (Cone/Bucket/Brick/ShoulderArmor/Crown/Newspaper), referenced by OTHER
 *     zombies via an RTID string like "RTID(ConeDefault@ArmorTypes)" instead
 *     of an inline HP number.
 *
 * There is also no "name" or "type" key anywhere in this file — the only
 * identifier is aliases[0] (e.g. "ZombieIceAgeHunter"). Annotating fields
 * cannot invent a type field that doesn't exist; ZombieFactory.resolveType()
 * does best-effort name matching on the alias instead (see ZombieFactory.java).
 *
 * Splitting zombies from armor-defs and resolving the RTID references is done
 * in ZombieRepository (see the companion patch to that file) — a single
 * ZombieTemplate cannot answer "what's my armor HP" on its own.
 */
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
    public Integer armorLayers;

    @SerializedName("ArmorLayerHealth")
    public Integer armorLayerHealth;

    @SerializedName("ArmorFlags")
    public List<String> armorFlags;

    @SerializedName("FireLayer")
    public Boolean fireLayer;
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
}