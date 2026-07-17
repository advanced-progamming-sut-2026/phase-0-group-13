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
  private final int yCoordinate;
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
      this.xCoordinate += speed;
    }
  }

  public void hitZombie(Zombie zombie) {
    if (!isActive || isFromZombie) return;

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

  public void hitPlant(Plant plant) {
    if (!isActive || !isFromZombie) return;
    plant.takeDamage(this.damage);
    this.isActive = false;
  }

  public double getXCoordinate() {
    return xCoordinate;
  }

  public int getYCoordinate() {
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
    if (effect == ProjectileEffect.FIRE || isFromZombie) {
      return this;
    }
    Projectile lit =
            new Projectile(damage, speed, xCoordinate, yCoordinate, ProjectileEffect.FIRE, piercing, lobbed, isFromZombie);
    lit.alreadyHit.addAll(this.alreadyHit);
    return lit;
  }
}