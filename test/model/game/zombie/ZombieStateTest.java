package model.game.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.enums.StatusEffect;
import model.game.zombie.ZombieParts.Armor;
import org.junit.jupiter.api.Test;

/**
 * Zombie's own state machine: armor absorption order, movement gating (eating/frozen/chilled/
 * hypnotized) and status effects. No board or behavior is needed for any of it.
 */
class ZombieStateTest {

  private static Zombie zombie(int hp, double speed) {
    return new Zombie("TestZombie", hp, speed, 2, 8.0, null);
  }

  // ---- damage / armor ---------------------------------------------------------------------

  @Test
  void takeDamageIgnoringArmorHitsHealthDirectly() {
    Zombie z = zombie(100, 0.1);
    z.addArmor(new Armor("cone", 50));
    z.takeDamage(30, true);
    assertEquals(70, z.getCurrentHealth(), "ignoresArmor bypasses the armor entirely");
    assertEquals(50, z.getRemainingArmorHealth(), "the armor itself is untouched");
  }

  @Test
  void armorAbsorbsDamageBeforeHealthDoes() {
    Zombie z = zombie(100, 0.1);
    z.addArmor(new Armor("cone", 50));
    z.takeDamage(30, false);
    assertEquals(100, z.getCurrentHealth(), "the armor took the whole hit");
    assertEquals(20, z.getRemainingArmorHealth());
  }

  @Test
  void damageOverflowsFromArmorIntoHealthOnceTheArmorBreaks() {
    Zombie z = zombie(100, 0.1);
    z.addArmor(new Armor("cone", 50));
    z.takeDamage(70, false);
    assertEquals(0, z.getRemainingArmorHealth());
    assertTrue(z.isArmorBroken());
    assertEquals(80, z.getCurrentHealth(), "the extra 20 carried through to health");
  }

  @Test
  void multipleArmorLayersAreConsumedInOrder() {
    Zombie z = zombie(100, 0.1);
    z.addArmor(new Armor("cone", 30));
    z.addArmor(new Armor("bucket", 40));
    z.takeDamage(50, false);
    // First layer (30) is fully spent, the other 20 spills into the second layer (40 -> 20 left).
    assertEquals(20, z.getRemainingArmorHealth());
    assertEquals(100, z.getCurrentHealth(), "still nothing reached the body");
  }

  @Test
  void healClampsAtMaxHealth() {
    Zombie z = zombie(100, 0.1);
    z.takeDamage(60, true);
    z.heal(1000);
    assertEquals(100, z.getCurrentHealth());
  }

  @Test
  void takeDamageNeverDropsHealthBelowZero() {
    Zombie z = zombie(30, 0.1);
    z.takeDamage(9999, true);
    assertEquals(0, z.getCurrentHealth());
    assertTrue(z.isDead());
  }

  // ---- movement -----------------------------------------------------------------------------

  @Test
  void moveAdvancesTowardsTheHouseByItsOwnSpeed() {
    Zombie z = zombie(100, 0.2);
    double before = z.getX();
    z.move();
    assertEquals(before - 0.2, z.getX(), 1e-9, "zombies walk toward decreasing x, at their speed");
  }

  @Test
  void aZombieThatIsEatingDoesNotMove() {
    Zombie z = zombie(100, 0.2);
    double before = z.getX();
    z.setEating(true);
    z.move();
    assertEquals(before, z.getX());
  }

  @Test
  void aFrozenZombieDoesNotMove() {
    Zombie z = zombie(100, 0.2);
    double before = z.getX();
    z.applyEffect(StatusEffect.FROZEN, 20);
    z.move();
    assertEquals(before, z.getX());
  }

  @Test
  void aChilledZombieMovesAtHalfSpeed() {
    Zombie z = zombie(100, 0.2);
    double before = z.getX();
    z.applyEffect(StatusEffect.CHILLED, 20);
    z.move();
    assertEquals(before - 0.1, z.getX(), 1e-9);
  }

  @Test
  void aHypnotizedZombieWalksTheOtherWay() {
    Zombie z = zombie(100, 0.2);
    double before = z.getX();
    z.setHypnotized(true);
    z.move();
    assertEquals(before + 0.2, z.getX(), 1e-9, "hypnotized zombies attack their own side");
  }

  @Test
  void extinguishFrozenStatusClearsBothFrozenAndChilled() {
    Zombie z = zombie(100, 0.2);
    z.applyEffect(StatusEffect.FROZEN, 20);
    z.applyEffect(StatusEffect.CHILLED, 20);
    z.extinguishFrozenStatus();
    double before = z.getX();
    z.move();
    assertEquals(before - 0.2, z.getX(), 1e-9, "back to full speed, unfrozen");
  }

  // ---- status effects ------------------------------------------------------------------------

  @Test
  void poisonTicksDamageEachTimeItProcesses() {
    Zombie z = zombie(100, 0.1);
    z.applyEffect(StatusEffect.POISONED, 3);
    z.update(0, boardStub());
    assertEquals(98, z.getCurrentHealth(), "poison deals 2 per processed tick, ignoring armor");
  }

  @Test
  void anEffectExpiresAfterItsGivenDuration() {
    Zombie z = zombie(100, 0.1);
    z.applyEffect(StatusEffect.CHILLED, 1);
    z.update(0, boardStub());
    assertFalse(z.getActiveEffects().containsKey(StatusEffect.CHILLED));
  }

  /** update() needs a Board, but nothing here plants or spawns anything on it. */
  private static model.game.Board boardStub() {
    return new model.game.Board(5, 9);
  }
}
