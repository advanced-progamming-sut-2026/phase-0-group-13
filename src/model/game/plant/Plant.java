package model.game.plant;

import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.Board;
import model.game.PlantFood;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.PlantAction;

public class Plant {
  private final int id;
  private final String name;
  private int currentHealth;
  private final int maxHealth;
  private final int cost;
  private final int level;

  private final int row;
  private final int col;
  private double x;
  private double y;

  private final PlantCategory category;
  private final EnumSet<PlantTag> tags;
  // حس میکنم tags بی فایده اس کلا وجودش و تاثیر خاصی نداره انچنان
  // بودنش بیهودس

  private final PlantAction behavior;
  private final PlantFood plantFood;
  private int lastActionTick;

  private final boolean isBoosted;

  public Plant(
          PlantTemplate template,
          int row,
          int col,
          PlantCategory category,
          EnumSet<PlantTag> tags,
          PlantAction behavior,
          PlantFood plantFood) {
    this.id = template.id;
    this.name = template.name;
    this.maxHealth = template.baseHp;
    this.currentHealth = template.baseHp;
    this.cost = template.cost;
    this.category = category;
    this.tags = tags;
    this.behavior = behavior;
    this.plantFood = plantFood;

    this.row = row;
    this.col = col;
    this.x = col;
    this.y = row;

    this.lastActionTick = 0;
    this.level = 1;
    this.isBoosted = false;
  }

  public void update(int currentTick, Board board) {
    if (isDead()) return;

    if (plantFood != null && plantFood.canExecute()) {
      plantFood.execute(this, board, currentTick);
      return;
    }

    if (behavior != null) {
      behavior.execute(this, board, currentTick);
    }
  }

  public void applyPlantFood() {
    if (plantFood != null) {
      plantFood.activate();
    } else {
      System.out.println(
              "Warning: " + name + " has no Plant Food effect configured; feed ignored.");
    }
  }

  public void takeDamage(int damage) {
    this.currentHealth = Math.max(0, this.currentHealth - damage);
  }

  public void changinCordinate(double x, double y) {
    this.x += x;
    this.y += y;
  }

  public boolean isDead() {
    return this.currentHealth <= 0;
  }

  public String getName() {
    return name;
  }

  public int getCurrentHealth() {
    return currentHealth;
  }

  public int getCost() {
    return cost;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int getLastActionTick() {
    return lastActionTick;
  }

  public void setLastActionTick(int tick) {
    this.lastActionTick = tick;
  }
}