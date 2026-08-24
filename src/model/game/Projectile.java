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
  private final double launchX;
  private double targetX;

  private final Set<Zombie> alreadyHit = new HashSet<>();
  // چند زامبی رو همین یه تیر (پیرسینگ/strike-through) کشته؛ برای امتیاز MULTI_KILL_ONE_SHOT
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
      this.xCoordinate += speed * stepCol;
      this.yCoordinate += speed * stepRow;
      if (remainingLifeTicks > 0) {
        remainingLifeTicks--;
      }
    }
  }

  // ---- انگورهای کمانه‌کننده Grapeshot ----

  /**
   * سقف تعداد زامبی‌هایی که یک تیر پیرسینگ می‌تواند از آن‌ها رد شود. صفر یعنی بی‌نهایت.
   *
   * <p>Cactus در plants.json «از ۳ زامبی رد می‌شود» ثبت شده (و Lvl 2 یکی اضافه می‌کند)، ولی
   * Fume-shroom سقفی ندارد؛ پس این محدودیت اختیاری است نه سراسری.
   */
  public Projectile withPierceLimit(int limit) {
    this.pierceLimit = Math.max(0, limit);
    return this;
  }

  public int getPierceLimit() {
    return pierceLimit;
  }

  /** شعاع پاشش برحسب خانه؛ صفر یعنی تیر فقط به همان هدف می‌خورد. */
  public Projectile withSplash(double tiles) {
    this.splashRadius = Math.max(0, tiles);
    return this;
  }

  public double getSplashRadius() {
    return splashRadius;
  }

  /** کره‌ی Kernel-pult: برخورد علاوه بر دمیج، زامبی را چند تیک میخکوب می‌کند. */
  public Projectile withStun(int ticks) {
    this.stunTicks = Math.max(0, ticks);
    return this;
  }

  /**
   * خانه‌ای که تیر کمانی به سمتش پرتاب شده. مدل همچنان مستقیم حرکت می‌کند؛ این فقط بازه‌ای است
   * که نمای گرافیکی قوس را رویش می‌کشد.
   */
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

  /** این پرتابه از دیواره‌ها کمانه می‌کند و بعد از {@code lifeTicks} تیک ناپدید می‌شود. */
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

  /** کمانه از سقف/کف زمین: جهت عمودی برعکس می‌شود. */
  public void bounceVertically(int rows) {
    stepRow = -stepRow;
    if (stepRow == 0) {
      stepRow = 1;
    }
    yCoordinate = Math.max(0, Math.min(rows - 1.0, yCoordinate + stepRow));
    alreadyHit.clear();
  }

  /** کمانه از دیوارهٔ چپ/راست: جهت افقی برعکس می‌شود. */
  public void bounceHorizontally(int columns) {
    stepCol = -stepCol;
    xCoordinate = Math.max(0, Math.min(columns - 1.0, xCoordinate + stepCol));
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

  /**
   * برخورد یک پرتابه‌ی انفجاری: اول هرچه در شعاع پاشش است، بعد خود هدف.
   *
   * <p>پاشش قبل از هدف اعمال می‌شود چون خوردن به هدف تیر غیرپیرسینگ را غیرفعال می‌کند.
   */
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
      // کره‌ی Kernel-pult: زامبی برای لحظه‌ای میخکوب می‌شود
      zombie.setEating(false);
      zombie.applyEffect(StatusEffect.FROZEN, stunTicks);
    }
  }

  // FIX (GDD Target 1.4 - Jester Zombie): قبلا این متود اصلا از هیچ‌جا صدا زده نمیشد (پرتابه‌های
  // زامبی‌محور هیچ‌وقت با گیاه برخورد نمیکردن)، پس افکت المنتال (یخ) هیچ‌وقت به گیاه نمیرسید. حالا هم
  // دمیج معمولی میزنه و هم - اگه یخی باشه - گیاه رو موقتا فریز میکنه (دقیقا مثل CHILLED زامبی‌ها،
  // همون مدت ۵۰ تیک)
  private static final int PLANT_FREEZE_DURATION_TICKS = 50;

  public void hitPlant(Plant plant, int currentTick) {
    if (!isActive || !isFromZombie) return;
    plant.takeDamage(this.damage);
    if (effect == ProjectileEffect.ICE) {
      // طبق داک، تیر یخی زامبی‌ها (شکارچی و تیرهای برگردانده‌شده‌ی ژانگولر) یک سطح یخ‌زدگی
      // اضافه می‌کند و گیاه در سومین برخورد کاملا یخ می‌زند
      plant.addFreezeExposure(1, currentTick, PLANT_FREEZE_DURATION_TICKS);
    }
    this.isActive = false;
  }

  public int getDamage() {
    return damage;
  }

  /** چند زامبی تاحالا با همین یه شلیک کشته شده (برای امتیاز MULTI_KILL_ONE_SHOT). */
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

  public int getYCoordinate() {
    return (int) Math.round(yCoordinate);
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
   * @param blueFlame شعله آبی Torchwood (غذای گیاه): چون FIRE موقع برخورد دمیج را ۲ برابر می‌کند،
   *     پایه را ۱.۵ برابر می‌کنیم تا مجموع ۳ برابر شود.
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
    lit.aimedAt(targetX);
    return lit;
  }
}
