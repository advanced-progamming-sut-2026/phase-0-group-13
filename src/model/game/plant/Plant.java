package model.game.plant;

import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.Board;
import model.game.PlantFood;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.PlantAction;

public class Plant {
  private final String name;
  private int currentHealth;
  private int maxHealth;
  private final int cost;
  private final int level;

  private final int row;
  private final int col;
  private double x;
  private double y;

  private final PlantCategory category;
  private final EnumSet<PlantTag> tags;
  private final PlantAction behavior;
  private final PlantFood plantFood;
  private int lastActionTick;

  private int disabledUntilTick = -1;
  private boolean cursed = false;
  private model.game.zombie.Zombie curseSource;

  /** طبق داک سه سطح یخ‌زدگی: دو سطح اول اثری ندارند، سطح سوم گیاه را کامل یخ می‌زند. */
  public static final int MAX_FREEZE_LEVEL = 3;
  /** طبق داک، یخِ روی گیاه ۶۰۰ جان دارد. */
  public static final int ICE_BLOCK_HEALTH = 600;
  private int freezeLevel = 0;
  private int frozenUntilTick = -1;
  private int iceHealth = 0;

  private boolean deathHookFired = false;

  private int lifespanTicks = -1;
  private int plantedTick = -1;

  public static final int MAX_STACK = 5;
  private int stackCount = 1;
  private Plant shield;
  private boolean blueFlame;

  public Plant(PlantTemplate template, int row, int col, PlantCategory category, EnumSet<PlantTag> tags,
               PlantAction behavior, PlantFood plantFood) {
    this(template, row, col, category, tags, behavior, plantFood, 1, template.baseHp, template.cost);
  }

  public Plant(PlantTemplate template, int row, int col, PlantCategory category, EnumSet<PlantTag> tags,
               PlantAction behavior, PlantFood plantFood, int level, int maxHealth, int cost) {
    this.name = template.name;
    this.maxHealth = maxHealth;
    this.currentHealth = maxHealth;
    this.cost = cost;
    this.category = category;
    this.tags = tags;
    this.behavior = behavior;
    this.plantFood = plantFood;
    this.row = row;
    this.col = col;
    this.x = col;
    this.y = row;
    this.lastActionTick = 0;
    this.level = level;
  }

  public void update(int currentTick, Board board) {
    if (isDead()) return;
    if (hasExpired(currentTick)) {
      System.out.printf("%s withered away after its lifespan ran out.%n", name);
      takeDamage(maxHealth);
      return;
    }
    if (isDisabled(currentTick) || isFrozen(currentTick)) return;

    if (plantFood != null && plantFood.canExecute()) {
      plantFood.execute(this, board, currentTick);
      return;
    }

    if (behavior != null) {
      behavior.execute(this, board, currentTick);
    }
  }

  public void applyCurse(model.game.zombie.Zombie source) {
    this.cursed = true;
    this.curseSource = source;
  }

  public boolean isCursed() {
    return cursed;
  }

  public void disableUntil(int tick) {
    this.disabledUntilTick = tick;
  }

  public boolean isDisabled(int currentTick) {
    if (cursed) {
      if (curseSource == null || curseSource.isDead()) {
        cursed = false;
        curseSource = null;
        return false;
      }
      return true;
    }
    return currentTick < disabledUntilTick;
  }

  public boolean isHeldByOctopus(int currentTick) {
    return !cursed && currentTick < disabledUntilTick;
  }

  public void freeze(int currentTick, int durationTicks) {
    this.frozenUntilTick = Math.max(frozenUntilTick, currentTick + durationTicks);
    this.freezeLevel = 0;
  }

  /**
   * دو سطح اول یخ‌زدگی هیچ اثری ندارند؛ سطح سوم گیاه را داخل یخ ۶۰۰ جانی حبس می‌کند. طبق داک این یخ
   * با تایمر آب نمی‌شود و باید با تیر گیاهان شکسته (یا با آتش ذوب) شود، پس {@code durationTicks}
   * فقط برای سازگاری با فراخوانی‌های قبلی (Hunter و پرتابه‌ها) نگه داشته شده است.
   */
  public void addFreezeExposure(int amount, int currentTick, int durationTicks) {
    if (isFrozen(currentTick)) return;

    this.freezeLevel = Math.min(MAX_FREEZE_LEVEL, this.freezeLevel + amount);
    if (this.freezeLevel >= MAX_FREEZE_LEVEL) {
      encaseInIce();
    }
  }

  public void encaseInIce() {
    if (iceHealth > 0) return;

    this.iceHealth = ICE_BLOCK_HEALTH;
    this.freezeLevel = 0;
    System.out.printf(
            "%s at (%d, %d) is frozen solid; the ice has %d hp.%n",
            name, col + 1, row + 1, ICE_BLOCK_HEALTH);
  }

  public void damageIce(int amount) {
    if (iceHealth <= 0 || amount <= 0) return;

    iceHealth = Math.max(0, iceHealth - amount);
    if (iceHealth == 0) {
      System.out.printf("The ice around %s at (%d, %d) shattered.%n", name, col + 1, row + 1);
    }
  }

  public void meltIce() {
    damageIce(iceHealth);
  }

  public int getIceHealth() { return iceHealth; }

  public boolean isFrozen(int currentTick) {
    return iceHealth > 0 || currentTick < frozenUntilTick;
  }

  public int getFreezeLevel() { return freezeLevel; }

  public boolean hasPlantFoodEffect() {
    return plantFood != null;
  }

  /**
   * True while a plant-food dose is still running.
   *
   * <p>Separate from {@link #hasPlantFoodEffect}, which only says the plant has an effect at all.
   * This is the live one, and it is what the renderer needs: every rig ships a {@code plantfood}
   * clip, and without knowing the dose is running there is no moment at which to play it.
   */
  public boolean isPlantFoodActive() {
    return plantFood != null && plantFood.canExecute();
  }

  public void applyPlantFood() {
    if (plantFood != null) {
      plantFood.activate();
    } else {
      System.out.println(name + " is a single-use plant and has no Plant Food effect.");
    }
  }

  public void takeDamage(int damage) {
    if (shield != null) {
      if (!shield.isDead()) {
        shield.takeDamage(damage);
        return;
      }
      shield = null;
    }
    this.currentHealth = Math.max(0, this.currentHealth - damage);
  }

  public void setLifespanTicks(int lifespanTicks) {
    this.lifespanTicks = lifespanTicks;
  }

  public int getLifespanTicks() {
    return lifespanTicks;
  }

  public void resetLifespan(int currentTick) {
    if (lifespanTicks > 0) {
      this.plantedTick = currentTick;
    }
  }

  private boolean hasExpired(int currentTick) {
    if (lifespanTicks <= 0) {
      return false;
    }
    if (plantedTick == -1) {
      plantedTick = currentTick;
      return false;
    }
    return currentTick - plantedTick >= lifespanTicks;
  }

  public boolean addStack() {
    if (stackCount >= MAX_STACK) {
      return false;
    }
    stackCount++;
    return true;
  }

  public int getStackCount() { return stackCount; }
  public Plant getShield() { return shield; }
  public void setShield(Plant shield) { this.shield = shield; }

  public boolean isBlueFlame() { return blueFlame; }

  public void setBlueFlame(boolean blueFlame) { this.blueFlame = blueFlame; }

  public void grantBonusHealth(int amount) {
    if (amount > 0) {
      this.maxHealth += amount;
      this.currentHealth += amount;
    }
  }

  public void heal(int amount) {
    if (amount > 0) this.currentHealth = Math.min(maxHealth, this.currentHealth + amount);
  }

  public void changeCoordinate(double deltaX, double deltaY) {
    this.x += deltaX;
    this.y += deltaY;
  }

  public void markDeathHookFired() { this.deathHookFired = true; }
  public boolean hasDeathHookFired() { return deathHookFired; }

  public boolean isDead() { return this.currentHealth <= 0; }
  public String getName() { return name; }
  public int getCurrentHealth() { return currentHealth; }
  public int getMaxHealth() { return maxHealth; }
  public int getCost() { return cost; }
  public int getLevel() { return level; }
  public PlantCategory getCategory() { return category; }
  public EnumSet<PlantTag> getTags() { return tags; }
  public int getRow() { return row; }
  public int getCol() { return col; }
  public double getX() { return x; }
  public double getY() { return y; }
  public int getLastActionTick() { return lastActionTick; }
  public void setLastActionTick(int tick) { this.lastActionTick = tick; }
  public PlantAction getBehavior() { return behavior; }
}
