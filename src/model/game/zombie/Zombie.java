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
  private String displayName;
  private int currentHealth;
  private final int maxHealth;
  private final double speed;
  private int row;
  private double x;
  private double y;

  public static final int NO_CELL = -1;

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
  private int rowSpan = 1;
  private boolean boss;
  private boolean fireImmune;
  private int icedOnCell = NO_CELL;
  private double thrownFromX;
  private int thrownTicks;
  private int thrownTotal;

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
    if (thrownTicks > 0) {
      thrownTicks--;
    }
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
        takeDamage(2, true);
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

  public int getIcedOnCell() {
    return icedOnCell;
  }

  public void setIcedOnCell(int cell) {
    this.icedOnCell = cell;
  }

  public void markThrownFrom(double fromX, int ticks) {
    this.thrownFromX = fromX;
    this.thrownTicks = Math.max(0, ticks);
    this.thrownTotal = this.thrownTicks;
  }

  public double getThrownFromX() {
    return thrownFromX;
  }

  public float flightProgress() {
    return thrownTotal <= 0 ? 0f : thrownTicks / (float) thrownTotal;
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

  public void setX(double x) { this.x = x; }
  public double getY() { return y; }
  public int getRow() { return row; }
  public void setRow(int row) { this.row = row; }

  public int getRowSpan() { return rowSpan; }

  public void setRowSpan(int rowSpan) { this.rowSpan = Math.max(1, rowSpan); }

  public int getBottomRow() { return row + rowSpan - 1; }

  public boolean occupiesRow(double lane) {
    if (rowSpan <= 1) {
      return row == lane;
    }
    return lane >= row && lane <= getBottomRow();
  }

  /** Dr. Zomboss. Mowers cannot touch it and the HUD shows its health instead of the wave meter. */
  public boolean isBoss() { return boss; }

  public void setBoss(boolean boss) { this.boss = boss; }

  /** The Imp Dragon: fire shots do nothing to it. */
  public boolean isFireImmune() { return fireImmune; }

  public void setFireImmune(boolean fireImmune) { this.fireImmune = fireImmune; }
  public ZombieAction getBehavior() { return behavior; }
  public String getName() { return name; }

  public String getDisplayName() { return displayName == null ? name : displayName; }

  public void setDisplayName(String displayName) { this.displayName = displayName; }

  public int getCurrentHealth() { return currentHealth; }
  public int getMaxHealth() { return maxHealth; }
  public boolean isEating() { return isEating; }
  public double getSpeed() { return speed; }
  public void setEating(boolean eating) { this.isEating = eating; }
  public List<Armor> getArmors() { return armors; }

  public int getRemainingArmorHealth() {
    int total = 0;
    for (Armor armor : armors) {
      if (!armor.isDestroyed()) {
        total += armor.getCurrentHealth();
      }
    }
    return total;
  }

  public int getMaxArmorHealth() {
    int total = 0;
    for (Armor armor : armors) {
      total += armor.getMaxHealth();
    }
    return total;
  }

  public int getArmorDamageTaken() {
    return getMaxArmorHealth() - getRemainingArmorHealth();
  }

  public int getBodyDamageTaken() {
    return maxHealth - currentHealth;
  }

  public boolean isArmorBroken() {
    return !armors.isEmpty() && !hasIntactArmor();
  }


  public Map<StatusEffect, Integer> getActiveEffects() { return activeEffects; }
}
