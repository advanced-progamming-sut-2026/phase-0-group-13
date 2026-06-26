package model.game.zombie.ZombieParts;

public class Armor {
  private String name;
  private int currentHealth;
  private int maxHealth;
  private boolean isMetallic;

  public Armor(String name, int maxHealth, boolean isMetallic) {
    this.name = name;
    this.maxHealth = maxHealth;
    this.currentHealth = maxHealth;
    this.isMetallic = isMetallic;
  }

  public Armor(String name, int maxHealth) {
    this(name, maxHealth, false);
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
}
