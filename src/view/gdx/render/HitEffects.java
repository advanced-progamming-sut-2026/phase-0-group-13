package view.gdx.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The short-lived reactions to being hit that the doc's polish list asks for: a flash on anything
 * that just took damage, and a small burst where a projectile stopped.
 */
public final class HitEffects {

  public static final float FLASH_SECONDS = 0.16f;

  public static final float BURST_SECONDS = 0.22f;

  public static final float DEATH_SECONDS = 0.5f;

  public static final float PICKUP_SECONDS = 1.1f;

  public static final float SPARK_SECONDS = 0.42f;

  private static final float BURST_MAX_RADIUS_FRACTION = 0.42f;

  private static final double LAWN_MARGIN = 0.4;

  public record Burst(double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / BURST_SECONDS);
    }

    public float radiusFraction() {
      return BURST_MAX_RADIUS_FRACTION * progress();
    }

    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  public record DeathPuff(double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / DEATH_SECONDS);
    }

    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  public record LootPickup(String kind, double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / PICKUP_SECONDS);
    }

    public float alpha() {
      return Math.max(0f, 1f - progress());
    }
  }

  public record Spark(String kind, double column, int row, float age) {

    public float progress() {
      return Math.min(1f, age / SPARK_SECONDS);
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
  private final List<Spark> sparks = new ArrayList<>();
  private final Map<Object, Integer> counters = new IdentityHashMap<>();
  private int freshDeaths;
  private int freshImpacts;

  private final Map<Object, Integer> seenHealth = new IdentityHashMap<>();
  private final Map<Object, double[]> seenProjectiles = new IdentityHashMap<>();
  private final Map<Object, Boolean> seenAliveState = new IdentityHashMap<>();

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

    for (int i = sparks.size() - 1; i >= 0; i--) {
      Spark spark = sparks.get(i);
      float age = spark.age() + delta;
      if (age >= SPARK_SECONDS) {
        sparks.remove(i);
      } else {
        sparks.set(i, new Spark(spark.kind(), spark.column(), spark.row(), age));
      }
    }
  }

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

  public void observeProjectile(Object projectile, double column, int row) {
    if (projectile == null) {
      return;
    }
    seenProjectiles.put(projectile, new double[] {column, row});
  }

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

  /**
   * Records a death seen from outside, by a caller that watched the zombie disappear.
   *
   * <p>{@link #observeZombieState} can only fire when a dead zombie is still in the board's list,
   * and it never is: Board.cleanupEntities drops it in the same tick it dies, so the alive-to-dead
   * transition happens entirely between frames. The renderer is the one that can tell -- it keeps
   * last frame's zombies -- so it reports the death here instead.
   */
  public void spawnDeathPuff(double column, int row) {
    deathPuffs.add(new DeathPuff(column, row, 0f));
    freshDeaths++;
  }

  public void spawnPickup(String kind, double column, int row) {
    pickups.add(new LootPickup(kind, column, row, 0f));
  }

  public void spawnSpark(String kind, double column, int row) {
    sparks.add(new Spark(kind, column, row, 0f));
  }

  /**
   * Offers a per-entity counter and fires a spark on the frame it goes down.
   *
   * @param key the entity the count belongs to, matched by identity
   */
  public void observeCount(Object key, int count, String sparkKind, double column, int row) {
    if (key == null) {
      return;
    }
    Integer before = counters.get(key);
    if (before != null && count < before) {
      spawnSpark(sparkKind, column, row);
    }
    counters.put(key, count);
  }

  /** Drops the counter for an entity that is gone, so its identity cannot be reused stale. */
  public void forgetCounts(java.util.function.Predicate<Object> gone) {
    counters.keySet().removeIf(gone);
  }

  public int drainFreshDeaths() {
    int count = freshDeaths;
    freshDeaths = 0;
    return count;
  }

  /** Projectiles that landed on something since the last call, for the impact sound effect. */
  public int drainFreshImpacts() {
    int count = freshImpacts;
    freshImpacts = 0;
    return count;
  }

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
        freshImpacts++;
      }
    }
    projectiles.clear();
    projectiles.putAll(seenProjectiles);
  }

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

  public List<Spark> getSparks() {
    return sparks;
  }

  public void clear() {
    health.clear();
    flash.clear();
    projectiles.clear();
    bursts.clear();
    aliveState.clear();
    deathPuffs.clear();
    pickups.clear();
    sparks.clear();
    counters.clear();
    freshDeaths = 0;
    freshImpacts = 0;
  }
}
