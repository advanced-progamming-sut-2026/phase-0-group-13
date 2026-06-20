package model.game.plant;

import model.enums.PlantCategory;
import model.enums.PlantType;
import model.enums.PlantTag;
import model.game.Board;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.PlantAction;
import java.util.EnumSet;

public class Plant {
    private int id;
    private String name;
    private int currentHealth;
    private int maxHealth;
    private int cost;
    private int level;

    private int row;
    private int col;
    private double x;
    private double y;

    private PlantCategory category;
    private EnumSet<PlantTag> tags;

    private PlantAction behavior;
    private int lastActionTick;

    private boolean isBoosted;
    private int plantFoodDuration;

    public Plant(PlantTemplate template, int row, int col, PlantCategory category, EnumSet<PlantTag> tags, PlantAction behavior) {
        this.id = template.id;
        this.name = template.name;
        this.maxHealth = template.baseHp;
        this.currentHealth = template.baseHp;
        this.cost = template.cost;
        this.category = category;
        this.tags = tags;
        this.behavior = behavior;

        this.row = row;
        this.col = col;
        this.x = col;
        this.y = row;

        this.lastActionTick = 0;
        this.level = 1;
        this.isBoosted = false;
        this.plantFoodDuration = 0;
    }

    public void update(int currentTick, Board board) {
        if (isDead()) return;

        if (plantFoodDuration > 0) {
            plantFoodDuration--;
        }

        if (behavior != null) {
            behavior.execute(this, board, currentTick);
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

    // --- Getters & Setters ---
    public String getName() { return name; }
    public int getCurrentHealth() { return currentHealth; }
    public int getCost() { return cost; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLastActionTick() { return lastActionTick; }
    public void setLastActionTick(int tick) { this.lastActionTick = tick; }
}