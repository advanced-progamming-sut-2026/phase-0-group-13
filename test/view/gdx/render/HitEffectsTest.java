package view.gdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The effect tracker is pure frame-to-frame bookkeeping over identities and integers, with no
 * LibGDX in it, so all of it can be driven directly. What matters is that it never invents an
 * effect: a new entity is not a hit, healing is not a hit, and a projectile that flew off the lawn
 * did not land on anything.
 */
class HitEffectsTest {

  private static final int COLUMNS = 9;

  /** Stand-ins for a plant, a zombie and a projectile: only their identity is used. */
  private static Object entity() {
    return new Object();
  }

  private static HitEffects frame(HitEffects effects, float delta) {
    effects.advance(delta);
    return effects;
  }

  @Test
  void anEntitySeenForTheFirstTimeIsNotFlashing() {
    HitEffects effects = new HitEffects();
    Object plant = entity();

    frame(effects, 0.016f).observe(plant, 300);
    effects.endFrame(COLUMNS);

    assertEquals(0f, effects.flashStrength(plant),
        "a plant that just appeared at full health has not been hit");
  }

  @Test
  void healthGoingDownStartsAFlashThatFades() {
    HitEffects effects = new HitEffects();
    Object zombie = entity();

    frame(effects, 0.016f).observe(zombie, 200);
    effects.endFrame(COLUMNS);
    assertEquals(0f, effects.flashStrength(zombie));

    frame(effects, 0.016f).observe(zombie, 160);
    effects.endFrame(COLUMNS);
    assertTrue(effects.flashStrength(zombie) > 0.9f, "the flash should start at full strength");

    // most of the way through its life
    frame(effects, HitEffects.FLASH_SECONDS * 0.75f).observe(zombie, 160);
    effects.endFrame(COLUMNS);
    float fading = effects.flashStrength(zombie);
    assertTrue(fading > 0f && fading < 0.5f, "the flash should be fading, was " + fading);

    frame(effects, HitEffects.FLASH_SECONDS).observe(zombie, 160);
    effects.endFrame(COLUMNS);
    assertEquals(0f, effects.flashStrength(zombie), "the flash should be over");
  }

  @Test
  void healingIsNotAHit() {
    HitEffects effects = new HitEffects();
    Object plant = entity();

    frame(effects, 0.016f).observe(plant, 100);
    effects.endFrame(COLUMNS);
    frame(effects, 0.016f).observe(plant, 300);
    effects.endFrame(COLUMNS);

    assertEquals(0f, effects.flashStrength(plant), "a wall-nut being repaired is not damage");
  }

  @Test
  void aProjectileThatVanishesOverTheLawnLeavesABurst() {
    HitEffects effects = new HitEffects();
    Object pea = entity();

    frame(effects, 0.016f).observeProjectile(pea, 4.0, 2);
    effects.endFrame(COLUMNS);
    assertTrue(effects.getBursts().isEmpty(), "a projectile still in flight is not an impact");

    // gone this frame: it hit something
    frame(effects, 0.016f);
    effects.endFrame(COLUMNS);

    assertEquals(1, effects.getBursts().size());
    HitEffects.Burst burst = effects.getBursts().get(0);
    assertEquals(4.0, burst.column());
    assertEquals(2, burst.row());
    assertEquals(0f, burst.progress(), "a fresh burst starts at the beginning of its life");
  }

  @Test
  void aProjectileThatFlewOffTheLawnLeavesNothing() {
    HitEffects effects = new HitEffects();
    Object pea = entity();

    frame(effects, 0.016f).observeProjectile(pea, COLUMNS + 3.0, 1);
    effects.endFrame(COLUMNS);
    frame(effects, 0.016f);
    effects.endFrame(COLUMNS);

    assertTrue(effects.getBursts().isEmpty(),
        "a shot that left the board past the far edge did not hit anything");
  }

  @Test
  void aBurstGrowsFadesAndThenGoesAway() {
    HitEffects effects = new HitEffects();
    Object pea = entity();
    frame(effects, 0.016f).observeProjectile(pea, 3.0, 0);
    effects.endFrame(COLUMNS);
    frame(effects, 0.016f);
    effects.endFrame(COLUMNS);

    float firstRadius = effects.getBursts().get(0).radiusFraction();
    float firstAlpha = effects.getBursts().get(0).alpha();

    frame(effects, HitEffects.BURST_SECONDS * 0.5f);
    effects.endFrame(COLUMNS);
    HitEffects.Burst later = effects.getBursts().get(0);
    assertTrue(later.radiusFraction() > firstRadius, "the ring should grow");
    assertTrue(later.alpha() < firstAlpha, "the ring should fade as it grows");

    frame(effects, HitEffects.BURST_SECONDS);
    effects.endFrame(COLUMNS);
    assertTrue(effects.getBursts().isEmpty(), "the burst should be gone by now");
  }

  @Test
  void clearDropsEverything() {
    HitEffects effects = new HitEffects();
    Object zombie = entity();
    frame(effects, 0.016f).observe(zombie, 200);
    effects.endFrame(COLUMNS);
    frame(effects, 0.016f).observe(zombie, 10);
    effects.endFrame(COLUMNS);
    assertTrue(effects.flashStrength(zombie) > 0f);

    effects.clear();

    assertEquals(0f, effects.flashStrength(zombie));
    assertFalse(effects.getBursts().size() > 0);
  }
}
