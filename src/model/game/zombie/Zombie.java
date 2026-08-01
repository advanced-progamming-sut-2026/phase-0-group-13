package model.game.zombie;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import model.enums.StatusEffect;
import model.game.Board;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.behavior.ZombieAction;

public class Zombie {
  private final String name;
  private int currentHealth;
  private final int maxHealth;
  private final double speed;
  private int row;
  private double x;
  private double y;

  private final List<Armor> armors;
  private final ZombieAction behavior;
  private final Map<StatusEffect, Integer> activeEffects;

  private boolean isEating;
  private boolean shieldBlocker;
  private double speedMultiplier;
  private boolean shiny;
  private boolean plantFoodDropped;
  private boolean lootDropped;
  private boolean hypnotized;
  private boolean submerged;

  public Zombie(String name, int health, double speed, int row, double startX, ZombieAction behavior) {
    this.name = name;
    this.maxHealth = health;
    this.currentHealth = health;
    this.speed = speed;
    this.row = row;
    this.x = startX;
    this.armors = new ArrayList<>();
    this.behavior = behavior;
    this.isEating = false;
    this.activeEffects = new EnumMap<>(StatusEffect.class);
    this.shieldBlocker = false;
    this.speedMultiplier = 1.0;
    this.shiny = false;
    this.plantFoodDropped = false;
    this.lootDropped = false;
    this.hypnotized = false;
    this.submerged = false;
  }

  public void update(int currentTick, Board board) {
    if (isDead()) return;
    processEffects();
    if (!activeEffects.containsKey(StatusEffect.FROZEN) && behavior != null) {
      behavior.execute(this, board, currentTick);
    }
  }

  private void processEffects() {
    Iterator<Map.Entry<StatusEffect, Integer>> it = activeEffects.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<StatusEffect, Integer> entry = it.next();
      StatusEffect effect = entry.getKey();
      int remainingTicks = entry.getValue() - 1;

      if (remainingTicks <= 0) {
        it.remove();
      } else {
        entry.setValue(remainingTicks);
      }

      if (effect == StatusEffect.POISONED) {
        takeDamage(2, true); // Fixed: Safely clamp health instead of direct modification
      }
    }
  }

  public void move() {
    if (!isEating && !activeEffects.containsKey(StatusEffect.FROZEN)) {
      double actualSpeed = activeEffects.containsKey(StatusEffect.CHILLED) ? speed / 2.0 : speed;
      double direction = hypnotized ? 1.0 : -1.0;
      this.x += direction * actualSpeed * speedMultiplier;
    }
  }

  public void takeDamage(int damage, boolean ignoresArmor) {
    if (ignoresArmor) {
      this.currentHealth -= damage;
    } else {
      int remainingDamage = damage;
      for (Armor armor : armors) {
        if (!armor.isDestroyed()) {
          remainingDamage = armor.takeDamage(remainingDamage);
          if (remainingDamage <= 0) break;
        }
      }
      if (remainingDamage > 0) this.currentHealth -= remainingDamage;
    }
    this.currentHealth = Math.max(0, this.currentHealth);
  }

  public void heal(int amount) {
    if (amount > 0) this.currentHealth = Math.min(maxHealth, currentHealth + amount);
  }

  public void applyEffect(StatusEffect effect, int durationInTicks) {
    this.activeEffects.put(effect, durationInTicks);
  }

  public void extinguishFrozenStatus() {
    activeEffects.remove(StatusEffect.FROZEN);
    activeEffects.remove(StatusEffect.CHILLED);
  }

  public boolean hasIntactArmor() {
    for (Armor armor : armors) {
      if (!armor.isDestroyed()) return true;
    }
    return false;
  }

  public void addArmor(Armor armor) { if (armor != null) this.armors.add(armor); }
  public boolean hasDroppedLoot() { return lootDropped; }
  public void markLootDropped() { this.lootDropped = true; }
  public boolean isShiny() { return shiny; }
  public void setShiny(boolean shiny) { this.shiny = shiny; }
  public boolean hasDroppedPlantFood() { return plantFoodDropped; }
  public void markPlantFoodDropped() { this.plantFoodDropped = true; }
  public boolean isHypnotized() { return hypnotized; }
  public void setHypnotized(boolean hypnotized) { this.hypnotized = hypnotized; }
  public boolean isSubmerged() { return submerged; }
  public void setSubmerged(boolean submerged) { this.submerged = submerged; }
  public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
  public boolean hasShieldBlocker() { return shieldBlocker; }
  public void setShieldBlocker(boolean shieldBlocker) { this.shieldBlocker = shieldBlocker; }
  public boolean isDead() { return this.currentHealth <= 0; }
  public double getX() { return x; }
  public double getY() { return y; }
  public int getRow() { return row; }
  public void setRow(int row) { this.row = row; }
  public ZombieAction getBehavior() { return behavior; }
  public String getName() { return name; }
  public int getCurrentHealth() { return currentHealth; }
  public int getMaxHealth() { return maxHealth; }
  public boolean isEating() { return isEating; }
  public void setEating(boolean eating) { this.isEating = eating; }
  public List<Armor> getArmors() { return armors; }
  public Map<StatusEffect, Integer> getActiveEffects() { return activeEffects; }
}