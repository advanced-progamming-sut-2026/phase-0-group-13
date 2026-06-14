package model.plant;

import model.plant.PlantParts.PlantTemplate;

public class Plant {
    private int id;
    private String name;
    private int currentHealth;
    private int cost;
    private int row;
    private int col;
    private double x ;
    private double y;
    private String category;

    public Plant(PlantTemplate template, int row, int col) {
        this.id = template.id;
        this.name = template.name;
        this.currentHealth = template.baseHp;
        this.cost = template.cost;
        this.category = template.category;
        this.row = row;
        this.col = col;
        this.x = 0.0 ;
        this.y = 0.0;
    }
    public void takeDamage(int damage) {
        this.currentHealth -= damage;
        if (this.currentHealth < 0) {
            this.currentHealth = 0;
        }
    }
    public void changinCordinate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public boolean isDead() {
        return this.currentHealth <= 0;
    }

    public String getName() { return name; }
    public int getCurrentHealth() { return currentHealth; }
    public int getCost() { return cost; }
    public int getRow() { return row; }
    public int getCol() { return col; }
}
