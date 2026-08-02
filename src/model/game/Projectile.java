package model.game;

import java.util.HashSet;
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

  private final Set<Zombie> alreadyHit = new HashSet<>();

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
    }
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

    int finalDamage = (effect == ProjectileEffect.FIRE) ? damage * 2 : damage;
    boolean ignoresArmor = (effect == ProjectileEffect.POISON);

    zombie.takeDamage(finalDamage, ignoresArmor);

    if (effect == ProjectileEffect.ICE) {
      zombie.applyEffect(StatusEffect.CHILLED, 50);
    }
    if (effect == ProjectileEffect.FIRE) {
      zombie.extinguishFrozenStatus();
    }

    alreadyHit.add(zombie);

    if (!piercing) {
      this.isActive = false;
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
      plant.freeze(currentTick, PLANT_FREEZE_DURATION_TICKS);
    }
    this.isActive = false;
  }

  public int getDamage() {
    return damage;
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
    return lit;
  }
}