package model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import model.core.GameManager;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.TileEffects.TileEffect;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import org.junit.jupiter.api.Test;

/**
 * GameManager.placePlant end to end, against a real Board -- not the terminal controller, not
 * GdxGameActions, the actual rule engine both of them call. Plant templates are built by hand
 * rather than loaded from plants.json, so these stay true regardless of which real plant currently
 * happens to carry which tag.
 */
class BoardPlantingRulesTest {

  private static GameManager runningMatch() {
    GameManager match = new GameManager();
    match.initializeLevel(5, 9, new ArrayList<>());
    match.startGame();
    return match;
  }

  /**
   * row/col are load-bearing, not decoration: Board.getPlantAt matches a plant by its own row/col
   * fields, which are fixed at construction, so a plant built for one tile and placed at another
   * would look empty to every occupancy check placePlant makes -- stacking, shielding, water, all
   * of it. Every call site below passes the same coordinates it places the plant at.
   */
  private static Plant plant(String name, int row, int col, int cost, PlantCategory category,
      PlantTag... tags) {
    PlantTemplate template = new PlantTemplate();
    template.name = name;
    template.baseHp = 100;
    template.cost = cost;
    EnumSet<PlantTag> tagSet = tags.length == 0
        ? EnumSet.noneOf(PlantTag.class)
        : EnumSet.copyOf(java.util.List.of(tags));
    return new Plant(template, row, col, category, tagSet, null, null);
  }

  // ---- basic gating -------------------------------------------------------------------------

  @Test
  void placingBeforeTheMatchStartsIsRefused() {
    GameManager match = new GameManager();
    match.initializeLevel(5, 9, new ArrayList<>());
    // startGame() is never called.
    assertFalse(match.placePlant(plant("peashooter", 2, 3, 100, PlantCategory.SHOOTER), 2, 3));
  }

  @Test
  void placingOutOfBoundsIsRefused() {
    GameManager match = runningMatch();
    assertFalse(match.placePlant(plant("peashooter", -1, 0, 50, PlantCategory.SHOOTER), -1, 0));
    assertFalse(match.placePlant(plant("peashooter", 0, 9, 50, PlantCategory.SHOOTER), 0, 9));
    assertFalse(match.placePlant(plant("peashooter", 5, 0, 50, PlantCategory.SHOOTER), 5, 0));
  }

  @Test
  void aTileThatBlocksPlantingRefusesEvenAnEmptyTile() {
    GameManager match = runningMatch();
    Tile tile = match.getBoard().getTile(1, 1);
    tile.setEffect(new TileEffect("gravestone", -1) {
      @Override
      public boolean blocksPlanting() {
        return true;
      }
    });

    assertFalse(match.placePlant(plant("peashooter", 1, 1, 50, PlantCategory.SHOOTER), 1, 1));
  }

  // ---- sun economy --------------------------------------------------------------------------

  @Test
  void placingDeductsTheSunCost() {
    GameManager match = runningMatch();
    int before = match.getSunAmount();
    assertTrue(before >= 50, "test assumes the board's starting sun");

    assertTrue(match.placePlant(plant("cheap-plant", 2, 3, 50, PlantCategory.SHOOTER), 2, 3));

    assertEquals(before - 50, match.getSunAmount());
  }

  @Test
  void placingWithoutEnoughSunIsRefusedAndNothingIsDeducted() {
    GameManager match = runningMatch();
    int before = match.getSunAmount();

    assertFalse(match.placePlant(
        plant("expensive-plant", 2, 3, before + 1, PlantCategory.SHOOTER), 2, 3));

    assertEquals(before, match.getSunAmount(), "a refused placement must not spend anything");
    assertNull(match.getBoard().getPlantAt(2, 3));
  }

  @Test
  void freePlantingBypassesTheSunCostEntirely() {
    GameManager match = runningMatch();
    match.enableFreePlanting();
    int before = match.getSunAmount();

    assertTrue(match.placePlant(plant("free-plant", 2, 3, before + 500, PlantCategory.SHOOTER), 2, 3));

    assertEquals(before, match.getSunAmount());
  }

  // ---- water ----------------------------------------------------------------------------------

  @Test
  void anAquaticPlantCannotBePlacedOnDryLand() {
    GameManager match = runningMatch();
    assertFalse(match.getBoard().isWaterAt(2, 3));
    assertFalse(match.placePlant(
        plant("lily-pad", 2, 3, 25, PlantCategory.MODIFIER, PlantTag.WATER), 2, 3));
  }

  @Test
  void anAquaticPlantCanBePlacedOnWater() {
    GameManager match = runningMatch();
    match.getBoard().setWaterAt(2, 3, true);
    assertTrue(match.placePlant(
        plant("lily-pad", 2, 3, 25, PlantCategory.MODIFIER, PlantTag.WATER), 2, 3));
  }

  @Test
  void aLandPlantCannotBePlacedDirectlyOnOpenWater() {
    GameManager match = runningMatch();
    match.getBoard().setWaterAt(2, 3, true);
    assertFalse(match.placePlant(plant("peashooter", 2, 3, 50, PlantCategory.SHOOTER), 2, 3));
  }

  @Test
  void aLandPlantCanBePlacedOnWaterOnceALilyPadIsThere() {
    GameManager match = runningMatch();
    match.enableFreePlanting(); // this test is about water rules, not the sun budget
    match.getBoard().setWaterAt(2, 3, true);
    assertTrue(match.placePlant(
        plant("lily-pad", 2, 3, 25, PlantCategory.MODIFIER, PlantTag.WATER), 2, 3));

    assertTrue(match.placePlant(plant("peashooter", 2, 3, 50, PlantCategory.SHOOTER), 2, 3),
        "a lily pad is what makes a water tile plantable for land plants");
    assertEquals(2, match.getBoard().getPlants().size(), "both plants now occupy that tile");
  }

  // ---- occupied tiles / stacking ----------------------------------------------------------

  @Test
  void aTileAlreadyHoldingAnOrdinaryPlantRefusesAnotherOne() {
    GameManager match = runningMatch();
    assertTrue(match.placePlant(plant("wall-nut", 2, 3, 50, PlantCategory.WALL_NUT), 2, 3));

    assertFalse(match.placePlant(plant("peashooter", 2, 3, 50, PlantCategory.SHOOTER), 2, 3));
  }

  @Test
  void twoOfTheSameStackTaggedPlantCombineIntoOneStack() {
    GameManager match = runningMatch();
    match.enableFreePlanting(); // this test is about stacking, not the sun budget
    assertTrue(match.placePlant(
        plant("pea-pod", 2, 3, 50, PlantCategory.SHOOTER, PlantTag.STACK), 2, 3));

    assertTrue(match.placePlant(
        plant("pea-pod", 2, 3, 50, PlantCategory.SHOOTER, PlantTag.STACK), 2, 3));

    assertEquals(1, match.getBoard().getPlants().size(), "stacking does not add a second entity");
    assertEquals(2, match.getBoard().getPlantAt(2, 3).getStackCount());
  }

  @Test
  void stackingStopsAtTheCap() {
    GameManager match = runningMatch();
    match.enableFreePlanting();
    for (int i = 0; i < Plant.MAX_STACK; i++) {
      assertTrue(match.placePlant(
          plant("pea-pod", 2, 3, 0, PlantCategory.SHOOTER, PlantTag.STACK), 2, 3));
    }

    assertFalse(match.placePlant(plant("pea-pod", 2, 3, 0, PlantCategory.SHOOTER, PlantTag.STACK), 2, 3),
        "the stack is already at its cap");
    assertEquals(Plant.MAX_STACK, match.getBoard().getPlantAt(2, 3).getStackCount());
  }

  @Test
  void aWallNutTaggedStackPlantShieldsADifferentPlantThatHasNoShieldYet() {
    GameManager match = runningMatch();
    match.enableFreePlanting(); // this test is about the shield rule, not the sun budget
    assertTrue(match.placePlant(plant("bonk-choy", 2, 3, 50, PlantCategory.MELEE), 2, 3));

    assertTrue(match.placePlant(
        plant("pumpkin", 2, 3, 25, PlantCategory.WALL_NUT, PlantTag.STACK), 2, 3));

    Plant guarded = match.getBoard().getPlantAt(2, 3);
    assertNotNull(guarded.getShield());
    assertEquals("pumpkin", guarded.getShield().getName());
    assertEquals(2, match.getBoard().getPlants().size(), "the shield is its own entity on the board");
  }

  @Test
  void aSecondShieldIsRefusedOnceOneIsAlreadyThere() {
    GameManager match = runningMatch();
    match.enableFreePlanting();
    match.placePlant(plant("bonk-choy", 2, 3, 0, PlantCategory.MELEE), 2, 3);
    match.placePlant(plant("pumpkin", 2, 3, 0, PlantCategory.WALL_NUT, PlantTag.STACK), 2, 3);

    assertFalse(match.placePlant(plant("pumpkin", 2, 3, 0, PlantCategory.WALL_NUT, PlantTag.STACK), 2, 3),
        "one pumpkin at a time");
  }

  @Test
  void aNonStackTaggedPlantCannotStackOnAnything() {
    GameManager match = runningMatch();
    match.enableFreePlanting();
    match.placePlant(plant("wall-nut", 2, 3, 0, PlantCategory.WALL_NUT), 2, 3);

    assertFalse(match.placePlant(plant("wall-nut", 2, 3, 0, PlantCategory.WALL_NUT), 2, 3),
        "without the STACK tag, a same-named plant still cannot double up");
  }
}
