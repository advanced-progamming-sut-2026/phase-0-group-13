package model.game.TileEffects;

public class TombStoneEffect extends TileEffect {

  // طبق داک روی این خانه نمی‌توان گیاه کاشت
  @Override
  public boolean blocksPlanting() {
    return isActive();
  }

  private int health;
  private final int maxHealth;
  private boolean blocksShots;

  private final boolean necromancy;
  private final int necromancyIntervalTicks;
  private int lastRaiseTick = -1;
  // داک (دوران تاریکی): بعضی سنگ‌قبرها ۵۰ خورشید یا یک غذای گیاه داخلشان است -> "SUN"/"PLANT_FOOD"
  private String buriedReward;

  public TombStoneEffect(int health, boolean blocksShots) {
    this(health, blocksShots, false, 0);
  }

  // necromancy=true یعنی این سنگ‌قبر هر necromancyIntervalTicks یه زامبی از دلش زنده میکنه (تا وقتی
  // خودش با شلیک کافی نابود بشه)
  public TombStoneEffect(int health, boolean blocksShots, boolean necromancy, int necromancyIntervalTicks) {
    super("Tombstone", -1);
    this.health = health;
    this.maxHealth = Math.max(1, health);
    this.blocksShots = blocksShots;
    this.necromancy = necromancy;
    this.necromancyIntervalTicks = necromancyIntervalTicks;
  }

  public void takeDamage(int damage) {
    if (!isActive()) return;

    this.health = Math.max(0, this.health - damage);
    System.out.println("Tombstone took " + damage + " damage. Remaining HP: " + this.health);

    if (this.health <= 0) {
      breakStone();
    }
  }

  public void breakStone() {
    System.out.println("The Tombstone has been completely destroyed!");
    this.blocksShots = false;
    remove();
  }

  public boolean isDueForNecromancy(int currentTick) {
    if (!necromancy || !isActive()) {
      return false;
    }
    if (lastRaiseTick == -1) {
      lastRaiseTick = currentTick;
      return false;
    }
    return currentTick - lastRaiseTick >= necromancyIntervalTicks;
  }

  public void markRaised(int currentTick) {
    this.lastRaiseTick = currentTick;
  }

  public boolean isBlocksShots() {
    return blocksShots;
  }

  public boolean isNecromancy() {
    return necromancy;
  }

  public String getBuriedReward() {
    return buriedReward;
  }

  public void setBuriedReward(String buriedReward) {
    this.buriedReward = buriedReward;
  }

  /** جایزهٔ داخل قبر را یک‌بار تحویل می‌دهد. */
  public String claimReward() {
    String reward = buriedReward;
    buriedReward = null;
    return reward;
  }

  public int getHealth() {
    return health;
  }

  /** What it started with, so a view can show how far through it is. Never zero. */
  public int getMaxHealth() {
    return maxHealth;
  }
}
