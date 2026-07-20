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

  private int disabledUntilTick = -1;

  // FIX (GDD Target 1.6 - Wizard Zombie): قبلا طلسم فقط یه تایمر ثابت بود؛ حالا به عمر خود Wizard
  // گره خورده - وقتی اون Wizard خاص کشته بشه، طلسم بلافاصله (همون تیک) باطل میشه
  private boolean cursed = false;
  private model.game.zombie.Zombie curseSource;

  // FIX (GDD Target 2.1 - Frostbite Caves Ice Winds): سطح یخ‌زدگی تجمعی؛ وقتی به سقف برسه، گیاه
  // برای مدتی کاملا فریز میشه (نمیتونه تیر بزنه/خورشید بسازه و ...)
  public static final int MAX_FREEZE_LEVEL = 100;
  private int freezeLevel = 0;
  private int frozenUntilTick = -1;

  private boolean deathHookFired = false;

  public Plant(
          PlantTemplate template,
          int row,
          int col,
          PlantCategory category,
          EnumSet<PlantTag> tags,
          PlantAction behavior,
          PlantFood plantFood) {
    this(template, row, col, category, tags, behavior, plantFood, 1, template.baseHp, template.cost);
  }

  public Plant(
          PlantTemplate template,
          int row,
          int col,
          PlantCategory category,
          EnumSet<PlantTag> tags,
          PlantAction behavior,
          PlantFood plantFood,
          int level,
          int maxHealth,
          int cost) {
    this.id = template.id;
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
    this.isBoosted = false;
  }

  public void update(int currentTick, Board board) {
    if (isDead()) return;
    if (currentTick < disabledUntilTick) return;
    if (isFrozen(currentTick)) return;

    if (plantFood != null && plantFood.canExecute()) {
      plantFood.execute(this, board, currentTick);
      return;
    }

    if (behavior != null) {
      behavior.execute(this, board, currentTick);
    }
  }

  // FIX (GDD Target 1.6 - Wizard Zombie): طلسم رو به عمر همون زامبی Wizard خاص گره میزنه؛ اگه اون
  // Wizard بمیره (یا اصلا وجود نداشته باشه)، طلسم فورا باطل میشه، نه با یه تایمر ثابت
  public void applyCurse(model.game.zombie.Zombie source) {
    this.cursed = true;
    this.curseSource = source;
  }

  public boolean isCursed() {
    return cursed;
  }

  // برای گیاه‌هایی مثل Wizard که به‌جای خوردن، گیاه رو موقتا از کار میندازن (تبدیل به گوسفند و ...)
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

  public void freeze(int currentTick, int durationTicks) {
    this.frozenUntilTick = Math.max(frozenUntilTick, currentTick + durationTicks);
    this.freezeLevel = 0;
  }
  public void addFreezeExposure(int amount, int currentTick, int durationTicks) {
    if (isFrozen(currentTick)) {
      return;
    }
    this.freezeLevel = Math.min(MAX_FREEZE_LEVEL, this.freezeLevel + amount);
    if (this.freezeLevel >= MAX_FREEZE_LEVEL) {
      freeze(currentTick, durationTicks);
    }
  }

  public boolean isFrozen(int currentTick) {
    return currentTick < frozenUntilTick;
  }

  public int getFreezeLevel() {
    return freezeLevel;
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

  public void heal(int amount) {
    if (amount > 0) {
      this.currentHealth = Math.min(maxHealth, this.currentHealth + amount);
    }
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getLevel() {
    return level;
  }

  public PlantCategory getCategory() {
    return category;
  }

  public EnumSet<PlantTag> getTags() {
    return tags;
  }

  public boolean hasDeathHookFired() {
    return deathHookFired;
  }

  public void markDeathHookFired() {
    this.deathHookFired = true;
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