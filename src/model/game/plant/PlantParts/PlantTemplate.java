package model.game.plant.PlantParts;


import com.google.gson.annotations.SerializedName;

public class PlantTemplate {

  @SerializedName("ID")
  public int id;

  @SerializedName("Name")
  public String name;

  @SerializedName("Category")
  public String category;

  @SerializedName("Tags")
  public String tags;

  @SerializedName("Cost")
  public int cost;

  @SerializedName("Base HP")
  public int baseHp;

  @SerializedName("Damage")
  public String damage;

  @SerializedName("Base Ability")
  public String baseAbility;

  @SerializedName("Plant Food Effect")
  public String plantFoodEffect;

  @SerializedName("Lvl 2")
  public String lvl2;

  @SerializedName("Lvl 3")
  public String lvl3;

  @SerializedName("Lvl 4")
  public String lvl4;

  @SerializedName("Action Interval (s)")
  public String actionInterval;

  @SerializedName("Recharge (s)")
  public int recharge;
}