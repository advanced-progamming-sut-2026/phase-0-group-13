package model.game.zombie.ZombieParts;

import model.enums.ArmorType;

public class Armor {
  private final String name;
  private int currentHealth;
  private final int maxHealth;
  private final boolean isMetallic;
  private final ArmorType type;

  public Armor(String name, int maxHealth, boolean isMetallic, ArmorType type) {
    this.name = name;
    this.maxHealth = maxHealth;
    this.currentHealth = maxHealth;
    this.isMetallic = isMetallic;
    this.type = type;
  }

  public Armor(String name, int maxHealth, boolean isMetallic) {
    this(name, maxHealth, isMetallic, null);
  }

  public Armor(String name, int maxHealth) {
    this(name, maxHealth, false, null);
  }

  public int takeDamage(int damage) {
    if (isDestroyed()) return damage;

    if (currentHealth >= damage) {
      currentHealth -= damage;
      return 0; // زره مقاومت کرد
    } else {
      int remainingDamage = damage - currentHealth;
      currentHealth = 0; // زره نابود شد
      return remainingDamage; // خروج دمیج اضافی
    }
  }

  public boolean isDestroyed() {
    return currentHealth <= 0;
  }

  public String getName() {
    return name;
  }

  public int getCurrentHealth() {
    return currentHealth;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public boolean isMetallic() {
    return isMetallic;
  }

  public ArmorType getType() {
    return type;
  }
}