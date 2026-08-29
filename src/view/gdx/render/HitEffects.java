package view.gdx.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The short-lived reactions to being hit that the doc's polish list asks for: a flash on anything
 * that just took damage, and a small burst where a projectile stopped.
 *
 * <p>Observed, not pushed. The model has no notion of a frame and should not grow one, so this
 * remembers what it saw last frame and works out the rest: health that went down is a hit, and a
 * projectile that was on the lawn last frame and is not on it now landed on something. That keeps
 * the whole effect layer inside the renderer, where a missing effect costs nothing and can never
 * change what the simulation does.
 *
 * <p>Entities are keyed by identity rather than by row and column: two plants swap tiles over a
 * match's life, and a zombie's column is a moving double. An IdentityHashMap also means a dead
 * entity's entry disappears with the entity, since the map is rebuilt from what is still on the
 * board each frame.
 */
public final class HitEffects {

  /** How long a hit flash lasts. Long enough to see at 60fps, short enough not to smear. */
  public static final float FLASH_SECONDS = 0.16f;

  /** How long a projectile's landing burst is drawn for. */
  public static final float BURST_SECONDS = 0.22f;

  /** How long a zombie's death puff is drawn for. */
  public static final float DEATH_SECONDS = 0.5f;

  /** How long a coin/pot/diamond drop sits on the lawn before it fades. */
  public static final float PICKUP_SECONDS = 1.1f;

  /** Rings any bigger than this read as an explosion rather than a pea landing. */
  private static final float BURST_MAX_RADIUS_FRACTION = 0.42f;

  /** A projectile that vanished outside this band flew off the lawn; it did not hit anything. */
  private static final double LAWN_MARGIN = 0.4;

  /** One fading burst: where it is, and how far through its life. */
  public record Burst(double column, int row, float age) {

    /** 0 at the moment of impact, 1 when it is about to disappear. */
    public float progress() {
      return Math.min(1f, age / BURST_SECONDS);
    }

    /** Fraction of a cell the ring should be drawn at right now. */
    public float radiusFraction() {
      return BURST_MAX_RADIUS_FRACTION * progress();
    }

    /** Fades out as it grows. */
    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  /** The puff where a zombie died: where it was, and how far through its life. */
  public record DeathPuff(double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / DEATH_SECONDS);
    }

    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  /** A coin/pot/diamond sitting on the lawn: what it is, where, and how far through its life. */
  public record LootPickup(String kind, double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / PICKUP_SECONDS);
    }

    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  private final Map<Object, Integer> health = new IdentityHashMap<>();
  private final Map<Object, Float> flash = new IdentityHashMap<>();
  private final Map<Object, double[]> projectiles = new IdentityHashMap<>();
  private final List<Burst> bursts = new ArrayList<>();

  private final Map<Object, Boolean> aliveState = new IdentityHashMap<>();
  private final List<DeathPuff> deathPuffs = new ArrayList<>();
  private final List<LootPickup> pickups = new ArrayList<>();
  private int freshDeaths;

  private final Map<Object, Integer> seenHealth = new IdentityHashMap<>();
  private final Map<Object, double[]> seenProjectiles = new IdentityHashMap<>();
  private final Map<Object, Boolean> seenAliveState = new IdentityHashMap<>();

  /** Ages every effect. Call once a frame, before the entities are offered. */
  public void advance(float delta) {
    seenHealth.clear();
    seenProjectiles.clear();
    seenAliveState.clear();

    flash.replaceAll((entity, left) -> left - delta);
    flash.values().removeIf(left -> left <= 0f);

    for (int i = bursts.size() - 1; i >= 0; i--) {
      Burst burst = bursts.get(i);
      float age = burst.age() + delta;
      if (age >= BURST_SECONDS) {
        bursts.remove(i);
      } else {
        bursts.set(i, new Burst(burst.column(), burst.row(), age));
      }
    }

    for (int i = deathPuffs.size() - 1; i >= 0; i--) {
      DeathPuff puff = deathPuffs.get(i);
      float age = puff.age() + delta;
      if (age >= DEATH_SECONDS) {
        deathPuffs.remove(i);
      } else {
        deathPuffs.set(i, new DeathPuff(puff.column(), puff.row(), age));
      }
    }

    for (int i = pickups.size() - 1; i >= 0; i--) {
      LootPickup pickup = pickups.get(i);
      float age = pickup.age() + delta;
      if (age >= PICKUP_SECONDS) {
        pickups.remove(i);
      } else {
        pickups.set(i, new LootPickup(pickup.kind(), pickup.column(), pickup.row(), age));
      }
    }
  }

  /**
   * Offers one living entity's current health.
   *
   * <p>Health that dropped since the last frame starts a flash. An entity seen for the first time
   * only records its health -- a plant appearing at full health has not been hit.
   */
  public void observe(Object entity, int currentHealth) {
    if (entity == null) {
      return;
    }
    Integer before = health.get(entity);
    if (before != null && currentHealth < before) {
      flash.put(entity, FLASH_SECONDS);
    }
    seenHealth.put(entity, currentHealth);
  }

  /** Offers one projectile still in flight. */
  public void observeProjectile(Object projectile, double column, int row) {
    if (projectile == null) {
      return;
    }
    seenProjectiles.put(projectile, new double[] {column, row});
  }

  /**
   * Offers one zombie's alive/dead state, every frame, dead or not.
   *
   * <p>Unlike {@link #observe}, this has to see the dead ones too, since a puff starts exactly on
   * the frame a zombie flips from alive to dead - after that the entity is never offered as alive
   * again, so there is nothing left to compare against and the puff would never fire.
   */
  public void observeZombieState(Object zombie, boolean isDead, double column, int row) {
    if (zombie == null) {
      return;
    }
    Boolean wasAlive = aliveState.get(zombie);
    if (Boolean.TRUE.equals(wasAlive) && isDead) {
      deathPuffs.add(new DeathPuff(column, row, 0f));
      freshDeaths++;
    }
    seenAliveState.put(zombie, !isDead);
  }

  /** Puts a coin/pot/diamond on the lawn at this spot. */
  public void spawnPickup(String kind, double column, int row) {
    pickups.add(new LootPickup(kind, column, row, 0f));
  }

  /** Zombies that died since the last call, for the renderer to turn into a screen shake. */
  public int drainFreshDeaths() {
    int count = freshDeaths;
    freshDeaths = 0;
    return count;
  }

  /**
   * Closes the frame: anything not offered this time is gone.
   *
   * <p>A projectile that disappeared over the lawn hit something and gets a burst; one that
   * disappeared past either edge simply flew off and gets nothing.
   */
  public void endFrame(int columns) {
    health.clear();
    health.putAll(seenHealth);

    aliveState.clear();
    aliveState.putAll(seenAliveState);

    for (Map.Entry<Object, double[]> entry : projectiles.entrySet()) {
      if (seenProjectiles.containsKey(entry.getKey())) {
        continue;
      }
      double column = entry.getValue()[0];
      if (column >= -LAWN_MARGIN && column <= columns - 1 + LAWN_MARGIN) {
        bursts.add(new Burst(column, (int) entry.getValue()[1], 0f));
      }
    }
    projectiles.clear();
    projectiles.putAll(seenProjectiles);
  }

  /** 1 at the instant of the hit, falling to 0; 0 for an entity that was not hit. */
  public float flashStrength(Object entity) {
    Float left = flash.get(entity);
    return left == null ? 0f : Math.max(0f, Math.min(1f, left / FLASH_SECONDS));
  }

  public List<Burst> getBursts() {
    return bursts;
  }

  public List<DeathPuff> getDeathPuffs() {
    return deathPuffs;
  }

  public List<LootPickup> getPickups() {
    return pickups;
  }

  /** Drops everything, for a screen that is starting a new match with the same renderer. */
  public void clear() {
    health.clear();
    flash.clear();
    projectiles.clear();
    bursts.clear();
    aliveState.clear();
    deathPuffs.clear();
    pickups.clear();
    freshDeaths = 0;
  }
}
