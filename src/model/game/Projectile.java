package model.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.enums.StatusEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class Projectile {

  public enum ProjectileEffect {
    NORMAL,
    FIRE,
    ICE,
    POISON
  }

  private final int damage;
  private final double speed;
  private double xCoordinate;
  private double yCoordinate;
  private double previousX;
  private double previousY;
  private int stepCol = 1;
  private int stepRow = 0;
  private final boolean isFromZombie;
  private boolean isActive;
  private final ProjectileEffect effect;
  private final boolean piercing;
  private final boolean lobbed;
  private boolean bouncing;
  private int remainingLifeTicks = -1;
  private int pierceLimit;
  private double splashRadius;
  private int stunTicks;
  private String sourceName;
  private final double launchX;
  private double targetX;

  private final Set<Zombie> alreadyHit = new HashSet<>();
  private int killCount = 0;

  public Projectile(
          int damage,
          double speed,
          double x,
          int y,
          ProjectileEffect effect,
          boolean piercing,
          boolean lobbed,
          boolean isFromZombie) {
    this.damage = damage;
    this.speed = speed;
    this.xCoordinate = x;
    this.yCoordinate = y;
    this.effect = effect == null ? ProjectileEffect.NORMAL : effect;
    this.piercing = piercing;
    this.lobbed = lobbed;
    this.isFromZombie = isFromZombie;
    this.isActive = true;
    this.launchX = x;
    this.targetX = x;
    this.previousX = x;
    this.previousY = y;
  }

  public Projectile(int damage, double speed, double x, int y, boolean isSlowing, boolean isFromZombie) {
    this(
            damage,
            speed,
            x,
            y,
            isSlowing ? ProjectileEffect.ICE : ProjectileEffect.NORMAL,
            false,
            false,
            isFromZombie);
  }

  public void move() {
    if (isActive) {
      this.previousX = xCoordinate;
      this.previousY = yCoordinate;
      this.xCoordinate += speed * stepCol;
      this.yCoordinate += speed * stepRow;
      if (remainingLifeTicks > 0) {
        remainingLifeTicks--;
      }
    }
  }


  public Projectile withPierceLimit(int limit) {
    this.pierceLimit = Math.max(0, limit);
    return this;
  }

  public int getPierceLimit() {
    return pierceLimit;
  }

  public Projectile withSplash(double tiles) {
    this.splashRadius = Math.max(0, tiles);
    return this;
  }

  public double getSplashRadius() {
    return splashRadius;
  }

  public Projectile withStun(int ticks) {
    this.stunTicks = Math.max(0, ticks);
    return this;
  }

  public int getStunTicks() {
    return stunTicks;
  }

  public Projectile firedBy(String plantName) {
    this.sourceName = plantName;
    return this;
  }

  public String getSourceName() {
    return sourceName;
  }

  public Projectile aimedAt(double column) {
    this.targetX = column;
    return this;
  }

  public double getLaunchX() {
    return launchX;
  }

  public double getTargetX() {
    return targetX;
  }

  public void makeBouncing(int lifeTicks) {
    this.bouncing = true;
    this.remainingLifeTicks = lifeTicks;
  }

  public boolean isBouncing() {
    return bouncing;
  }

  public boolean isExpired() {
    return remainingLifeTicks == 0;
  }

  public void bounceVertically(int rows) {
    stepRow = -stepRow;
    if (stepRow == 0) {
      stepRow = 1;
    }
    yCoordinate = Math.max(0, Math.min(rows - 1.0, yCoordinate + stepRow));
    previousY = yCoordinate;
    alreadyHit.clear();
  }

  public void bounceHorizontally(int columns) {
    stepCol = -stepCol;
    xCoordinate = Math.max(0, Math.min(columns - 1.0, xCoordinate + stepCol));
    previousX = xCoordinate;
    alreadyHit.clear();
  }

  public void hitZombie(Zombie zombie) {
    if (!isActive || isFromZombie) return;

    if (zombie.isSubmerged() && !lobbed) {
      return;
    }

    if (lobbed && zombie.hasShieldBlocker()) {
      return;
    }
    if (alreadyHit.contains(zombie)) {
      return;
    }

    applyHit(zombie);
    alreadyHit.add(zombie);

    if (!piercing) {
      this.isActive = false;
    } else if (pierceLimit > 0 && alreadyHit.size() >= pierceLimit) {
      this.isActive = false;
    }

  }

  public void hitArea(List<Zombie> others, Zombie landedOn) {
    if (!isActive || isFromZombie) return;

    if (splashRadius > 0) {
      for (Zombie other : others) {
        if (other != landedOn && !other.isDead() && other.getRow() == landedOn.getRow()
                && Math.abs(other.getX() - landedOn.getX()) <= splashRadius) {
          applyHit(other);
        }
      }
    }
    hitZombie(landedOn);
  }

  private void applyHit(Zombie zombie) {
    if (effect == ProjectileEffect.FIRE && zombie.isFireImmune()) {
      return;
    }
    int finalDamage = (effect == ProjectileEffect.FIRE) ? damage * 2 : damage;
    boolean ignoresArmor = (effect == ProjectileEffect.POISON);

    zombie.takeDamage(finalDamage, ignoresArmor);
    if (zombie.isDead()) {
      killCount++;
    }

    if (effect == ProjectileEffect.ICE) {
      zombie.applyEffect(StatusEffect.CHILLED, 50);
    }
    if (effect == ProjectileEffect.FIRE) {
      zombie.extinguishFrozenStatus();
    }
    if (stunTicks > 0) {
      zombie.setEating(false);
      zombie.applyEffect(StatusEffect.FROZEN, stunTicks);
    }
  }

  private static final int PLANT_FREEZE_DURATION_TICKS = 50;

  public void hitPlant(Plant plant, int currentTick) {
    if (!isActive || !isFromZombie) return;
    plant.takeDamage(this.damage);
    if (effect == ProjectileEffect.ICE) {
      // طبق داک، تیر یخی زامبی‌ها (شکارچی و تیرهای برگردانده‌شده‌ی ژانگولر) یک سطح یخ‌زدگی
      plant.addFreezeExposure(1, currentTick, PLANT_FREEZE_DURATION_TICKS);
    }
    this.isActive = false;
  }

  public int getDamage() {
    return damage;
  }

  public int getKillCount() {
    return killCount;
  }

  public void setDirection(int stepCol, int stepRow) {
    this.stepCol = stepCol;
    this.stepRow = stepRow;
  }

  public double getXCoordinate() {
    return xCoordinate;
  }

  /**
   * Where this shot sat at the previous tick. Rendering walks from here to the current position
   * over the frames of one tick, which is what stops a shot jumping half a tile at a time.
   */
  public double getPreviousX() {
    return previousX;
  }

  public double getPreviousY() {
    return previousY;
  }

  public int getYCoordinate() {
    return (int) Math.round(yCoordinate);
  }

  /** The unrounded row, so a ricochet crossing lanes can be drawn between them. */
  public double getExactY() {
    return yCoordinate;
  }

  public boolean isFromZombie() {
    return isFromZombie;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isPiercing() {
    return piercing;
  }

  public boolean isLobbed() {
    return lobbed;
  }

  public ProjectileEffect getEffect() {
    return effect;
  }


  public Projectile ignited() {
    return ignited(false);
  }

  /**
   *
   * @param blueFlame شعله آبی Torchwood (غذای گیاه): چون FIRE موقع برخورد دمیج را ۲ برابر می‌کند،
   * پایه را ۱.۵ برابر می‌کنیم تا مجموع ۳ برابر شود.
   */
  public Projectile ignited(boolean blueFlame) {
    if (effect == ProjectileEffect.FIRE || isFromZombie) {
      return this;
    }
    int litDamage = blueFlame ? Math.round(damage * 1.5f) : damage;
    Projectile lit =
            new Projectile(litDamage, speed,
                    xCoordinate, getYCoordinate(), ProjectileEffect.FIRE, piercing, lobbed, isFromZombie);
    lit.setDirection(stepCol, stepRow);
    lit.alreadyHit.addAll(this.alreadyHit);
    lit.withPierceLimit(pierceLimit);
    lit.withSplash(splashRadius);
    lit.withStun(stunTicks);
    lit.firedBy(sourceName);
    lit.aimedAt(targetX);
    lit.previousX = this.previousX;
    lit.previousY = this.previousY;
    return lit;
  }
}
