package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import org.junit.jupiter.api.Test;

/**
 * Plant's own state machine: damage/shield/heal, the freeze/ice ladder, the octopus hold, and the
 * Wizard's curse. Built with a bare template and no board, since none of this needs one.
 */
class PlantStateTest {

  private static Plant plant(int hp) {
    PlantTemplate template = new PlantTemplate();
    template.name = "peashooter";
    template.baseHp = hp;
    template.cost = 100;
    return new Plant(template, 2, 3, PlantCategory.SHOOTER, EnumSet.noneOf(PlantTag.class), null, null);
  }

  // ---- damage / shield --------------------------------------------------------------------

  @Test
  void takeDamageClampsAtZeroAndNeverGoesNegative() {
    Plant p = plant(50);
    p.takeDamage(1000);
    assertEquals(0, p.getCurrentHealth());
    assertTrue(p.isDead());
  }

  @Test
  void healClampsAtMaxHealth() {
    Plant p = plant(100);
    p.takeDamage(80);
    p.heal(1000);
    assertEquals(100, p.getCurrentHealth());
  }

  @Test
  void negativeOrZeroHealDoesNothing() {
    Plant p = plant(100);
    p.takeDamage(30);
    p.heal(0);
    p.heal(-5);
    assertEquals(70, p.getCurrentHealth());
  }

  /**
   * A shield (pumpkin on a wall-nut) takes the hit instead of the plant underneath, and once the
   * shield itself dies it falls off -- but the overflow does not carry through to the plant in
   * the same hit, so the exact same kill blow that finishes the shield leaves its ward untouched.
   */
  @Test
  void aShieldAbsorbsDamageInsteadOfThePlantUnderneath() {
    Plant guarded = plant(100);
    Plant shield = plant(30);
    guarded.setShield(shield);

    guarded.takeDamage(10);

    assertEquals(20, shield.getCurrentHealth(), "the shield took the hit");
    assertEquals(100, guarded.getCurrentHealth(), "the plant under it is untouched");
  }

  @Test
  void aShieldFallsOffOnceItDiesWithoutSplashingTheRemainderOntoThePlant() {
    Plant guarded = plant(100);
    Plant shield = plant(30);
    guarded.setShield(shield);

    guarded.takeDamage(50); // more than the shield has -- shield dies, this hit is spent on it

    assertTrue(shield.isDead());
    assertEquals(100, guarded.getCurrentHealth(), "this hit was absorbed, not carried through");

    guarded.takeDamage(10); // the next hit has nothing left to protect it
    assertEquals(90, guarded.getCurrentHealth());
  }

  // ---- stacking -----------------------------------------------------------------------------

  @Test
  void addStackStopsAtTheCap() {
    Plant p = plant(100);
    assertEquals(1, p.getStackCount(), "a fresh plant is already one head");
    for (int i = 0; i < Plant.MAX_STACK; i++) {
      p.addStack();
    }
    assertEquals(Plant.MAX_STACK, p.getStackCount());
    assertFalse(p.addStack(), "past the cap, addStack refuses and reports it");
  }

  // ---- freeze / ice -----------------------------------------------------------------------

  @Test
  void theFirstTwoFreezeLevelsHaveNoEffect() {
    Plant p = plant(100);
    p.addFreezeExposure(1, 0, 100);
    p.addFreezeExposure(1, 0, 100);
    assertEquals(2, p.getFreezeLevel());
    assertFalse(p.isFrozen(0), "levels one and two do nothing, per the doc");
    assertEquals(0, p.getIceHealth());
  }

  @Test
  void theThirdFreezeLevelEncasesThePlantInIce() {
    Plant p = plant(100);
    p.addFreezeExposure(3, 0, 100);
    assertTrue(p.isFrozen(0));
    assertEquals(Plant.ICE_BLOCK_HEALTH, p.getIceHealth());
  }

  @Test
  void iceHasToBeBrokenRatherThanTimingOut() {
    Plant p = plant(100);
    p.addFreezeExposure(Plant.MAX_FREEZE_LEVEL, 0, 100);
    assertTrue(p.isFrozen(50_000), "no duration was ever given to the ice; it does not expire");
    p.damageIce(Plant.ICE_BLOCK_HEALTH);
    assertFalse(p.isFrozen(50_000), "breaking it frees the plant immediately");
  }

  @Test
  void meltIceClearsItInOneCall() {
    Plant p = plant(100);
    p.encaseInIce();
    p.meltIce();
    assertEquals(0, p.getIceHealth());
    assertFalse(p.isFrozen(0));
  }

  @Test
  void aPlainFreezeExpiresOnItsOwnUnlikeIce() {
    Plant p = plant(100);
    p.freeze(0, 20);
    assertTrue(p.isFrozen(10));
    assertFalse(p.isFrozen(20), "the duration has run out");
  }

  // ---- octopus hold vs. curse -------------------------------------------------------------

  /**
   * Both make {@link Plant#isDisabled} true, but they must never be confused: a cursed plant is
   * drawn as a sheep, a held one wears the octopus that caught it. See EntityRenderer's
   * drawOctopusHold, which relies on this split to avoid putting an octopus on the sheep.
   */
  @Test
  void anOctopusHoldIsDisabledAndReadsAsHeld() {
    Plant p = plant(100);
    p.disableUntil(150);

    assertTrue(p.isDisabled(100));
    assertTrue(p.isHeldByOctopus(100));
  }

  @Test
  void aHoldLapsesOnItsOwn() {
    Plant p = plant(100);
    p.disableUntil(150);
    assertFalse(p.isDisabled(200));
    assertFalse(p.isHeldByOctopus(200));
  }

  @Test
  void aCurseIsDisabledButMustNotReadAsAnOctopusHold() {
    Plant p = plant(100);
    Zombie wizard = new Zombie("Wizard", 100, 0.1, 1, 8.0, null);
    p.applyCurse(wizard);

    assertTrue(p.isCursed());
    assertTrue(p.isDisabled(100));
    assertFalse(p.isHeldByOctopus(100), "a sheep is not a held plant");
  }

  @Test
  void aCurseClearsItselfOnceTheWizardDies() {
    Plant p = plant(100);
    Zombie wizard = new Zombie("Wizard", 10, 0.1, 1, 8.0, null);
    p.applyCurse(wizard);
    assertTrue(p.isDisabled(0));

    wizard.takeDamage(10, true);
    assertTrue(wizard.isDead());

    assertFalse(p.isDisabled(0), "the curse lifts once its caster is gone");
    assertFalse(p.isCursed());
  }

  @Test
  void hasPlantFoodEffectIsFalseForAPlantBuiltWithNone() {
    assertFalse(plant(100).hasPlantFoodEffect());
  }
}
