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
  private boolean hypnotising;
  private String sourceName;
  private double muzzleOffset;
  private int launchRow = -1;
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

  /**
   * How fast a diagonal shot crosses lanes, against how fast it travels forward.
   *
   * <p>A diagonal used to move a whole lane every two ticks, the same rate as it moved forward.
   * On a five-lane board that put a Roto-baga pea off the top or bottom edge four ticks after it
   * was fired -- barely two tiles -- so its shots blinked out next to the plant and it looked as
   * though it did nothing at all. At a quarter rate a diagonal crosses one lane per four tiles,
   * which is a shot that visibly travels and can actually reach something.
   */
  private static final double DIAGONAL_LANE_RATE = 0.25;

  public void move() {
    if (isActive) {
      this.previousX = xCoordinate;
      this.previousY = yCoordinate;
      this.xCoordinate += speed * stepCol;
      this.yCoordinate += speed * stepRow * (stepCol != 0 ? DIAGONAL_LANE_RATE : 1.0);
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

  /** Turns whatever it lands on, for Caulipower's charm. Carries no damage of its own. */
  public Projectile thatHypnotises() {
    this.hypnotising = true;
    return this;
  }

  public boolean isHypnotising() {
    return hypnotising;
  }

  public Projectile firedBy(String plantName) {
    this.sourceName = plantName;
    return this;
  }

  public String getSourceName() {
    return sourceName;
  }

  /**
   * Where in the plant this shot left from, as a fraction of a lane above the plant's centre.
   *
   * <p>Only the view reads it. A volley is fired on a single tick from a single tile, so without
   * it Repeater's two peas, Mega Gatling's four and a five-head Pea Pod's five are all drawn at
   * exactly the same point and the player sees one pea however many were really fired.
   */
  public Projectile fromMuzzle(double laneFraction) {
    this.muzzleOffset = laneFraction;
    return this;
  }

  public double getMuzzleOffset() {
    return muzzleOffset;
  }

  /**
   * The lane the plant that fired this actually stands in, when it is not the lane the shot is
   * travelling down.
   *
   * <p>For Threepeater and the other lane-spreading shooters. A shot for the lane above is created
   * already in that lane, so on screen it simply appeared there with nothing tying it to the plant
   * -- three peas in three lanes that could have come from anywhere. Read only by the view, which
   * starts the shot at the plant and lets it cross into its lane over the tick it was fired.
   *
   * @return the firing plant's row, or -1 when the shot was fired straight down its own lane
   */
  public int getLaunchRow() {
    return launchRow;
  }

  public Projectile launchedFromRow(int row) {
    this.launchRow = row == getYCoordinate() ? -1 : row;
    return this;
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
    // Goo Peashooter "ignores armour and deals damage over time". Only the first half happened:
    // POISONED was a status the zombie knew how to burn down but that nothing ever applied, so
    // the goo's whole second half was missing from the game.
    if (effect == ProjectileEffect.POISON) {
      zombie.applyEffect(StatusEffect.POISONED, POISON_DURATION_TICKS);
    }
    if (effect == ProjectileEffect.FIRE) {
      zombie.extinguishFrozenStatus();
    }
    if (stunTicks > 0) {
      zombie.setEating(false);
      zombie.applyEffect(StatusEffect.STUNNED, stunTicks);
    }
    if (hypnotising && !zombie.isHypnotized()) {
      zombie.setHypnotized(true);
      zombie.setEating(false);
    }
  }

  /** How long a goo hit keeps eating at a zombie, matching the chill a Snow Pea leaves. */
  private static final int POISON_DURATION_TICKS = 50;

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
    lit.fromMuzzle(muzzleOffset);
    lit.launchRow = launchRow;
    if (hypnotising) {
      lit.thatHypnotises();
    }
    lit.aimedAt(targetX);
    lit.previousX = this.previousX;
    lit.previousY = this.previousY;
    return lit;
  }
}
