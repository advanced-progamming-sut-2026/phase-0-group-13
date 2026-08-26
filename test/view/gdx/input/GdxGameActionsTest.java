package view.gdx.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import model.core.GameManager;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.SunType;
import model.game.Board;
import model.game.PlantFood;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import org.junit.jupiter.api.Test;

/**
 * GdxGameActions is the click-to-model plumbing GameActionBridge asks for; these exercise it
 * against a real GameManager/Board rather than mocks, since the whole point of the bridge is that
 * it calls the exact same model methods the terminal build's controller does.
 *
 * <p>plantAt is not covered here: it looks plant templates up through the live
 * GameDataManager.plantRepository static, and mutating that shared, process-wide field from a
 * test risks other tests (and would need restoring afterwards). The planting rules it delegates
 * to are covered directly against GameManager instead.
 */
class GdxGameActionsTest {

  private static GameManager runningMatch() {
    GameManager match = new GameManager();
    match.initializeLevel(5, 9, new ArrayList<>());
    match.startGame();
    return match;
  }

  private static Sun sunAt(Board board, int col, int row, SunType type, boolean falling) {
    Sun sun = new Sun(25, 500, type, falling);
    sun.changinCordinate(col, row);
    board.addSun(sun);
    return sun;
  }

  private static Plant plantAt(Board board, int row, int col, PlantFood food) {
    PlantTemplate template = new PlantTemplate();
    template.name = "peashooter";
    template.baseHp = 100;
    template.cost = 100;
    Plant plant = new Plant(template, row, col, PlantCategory.SHOOTER,
        EnumSet.noneOf(PlantTag.class), null, food);
    board.placePlant(plant);
    return plant;
  }

  // ---- sun collection: click vs. hover ------------------------------------------------------

  @Test
  void collectSunAtTakesAGroundedSunOnThatTile() {
    GameManager match = runningMatch();
    sunAt(match.getBoard(), 3, 2, SunType.NORMAL, false);
    int before = match.getSunAmount();
    GdxGameActions actions = new GdxGameActions(match, () -> { }, m -> { });

    assertTrue(actions.collectSunAt(2, 3));
    assertTrue(match.getSunAmount() > before);
  }

  @Test
  void collectSunByHoverTakesAGroundedSunTheSameAsAClick() {
    GameManager match = runningMatch();
    sunAt(match.getBoard(), 3, 2, SunType.NORMAL, false);
    int before = match.getSunAmount();
    GdxGameActions actions = new GdxGameActions(match, () -> { }, m -> { });

    assertTrue(actions.collectSunByHover(2, 3));
    assertTrue(match.getSunAmount() > before);
  }

  @Test
  void hoveringAnEmptyTileTakesNothing() {
    GameManager match = runningMatch();
    sunAt(match.getBoard(), 3, 2, SunType.NORMAL, false);
    GdxGameActions actions = new GdxGameActions(match, () -> { }, m -> { });

    assertFalse(actions.collectSunByHover(0, 0));
  }

  /**
   * The one case hover deliberately skips: a radioactive sun that is still falling detonates on
   * collection and damages nearby plants. Sweeping the mouse across the lawn must not trigger
   * that -- only a deliberate click may.
   */
  @Test
  void hoverNeverSweepsUpAStillFallingRadioactiveSun() {
    GameManager match = runningMatch();
    sunAt(match.getBoard(), 5, 1, SunType.RADIOACTIVE, true);
    GdxGameActions actions = new GdxGameActions(match, () -> { }, m -> { });

    assertFalse(actions.collectSunByHover(1, 5), "hover must leave a falling radioactive sun alone");
    assertTrue(actions.collectSunAt(1, 5), "a deliberate click still may");
  }

  @Test
  void hoverDoesCollectARadioactiveSunThatHasAlreadyLanded() {
    GameManager match = runningMatch();
    Sun landed = sunAt(match.getBoard(), 6, 4, SunType.RADIOACTIVE, false);
    GdxGameActions actions = new GdxGameActions(match, () -> { }, m -> { });

    assertFalse(landed.isFalling(), "test setup: this one must already be grounded");
    assertTrue(actions.collectSunByHover(4, 6));
  }

  // ---- pluck ----------------------------------------------------------------------------------

  @Test
  void pluckRemovesThePlantOnThatTile() {
    GameManager match = runningMatch();
    plantAt(match.getBoard(), 1, 4, null);

    assertTrue(actionsFor(match).pluckAt(1, 4));
    assertEquals(null, match.getBoard().getPlantAt(1, 4));
  }

  @Test
  void pluckingAnEmptyTileFails() {
    GameManager match = runningMatch();
    assertFalse(actionsFor(match).pluckAt(1, 4));
  }

  // ---- plant food -----------------------------------------------------------------------------

  @Test
  void feedingAPlantWithAFoodEffectActivatesItAndSpendsAPacket() {
    GameManager match = runningMatch();
    Plant plant = plantAt(match.getBoard(), 0, 0, new PlantFood(20, null));
    match.getBoard().getGameState().addPlantFood();
    int before = match.getPlantFoodCount();

    assertTrue(actionsFor(match).feedPlantAt(0, 0));
    assertTrue(plant.hasPlantFoodEffect());
    assertEquals(before - 1, match.getPlantFoodCount());
  }

  @Test
  void feedingAPlantWithNoFoodEffectIsRefused() {
    GameManager match = runningMatch();
    plantAt(match.getBoard(), 0, 0, null);
    match.getBoard().getGameState().addPlantFood();

    assertFalse(actionsFor(match).feedPlantAt(0, 0));
  }

  @Test
  void feedingAnEmptyTileIsRefused() {
    GameManager match = runningMatch();
    match.getBoard().getGameState().addPlantFood();
    assertFalse(actionsFor(match).feedPlantAt(0, 0));
  }

  // ---- exit -----------------------------------------------------------------------------------

  @Test
  void requestExitRunsWhateverTheScreenGaveIt() {
    List<Boolean> called = new ArrayList<>();
    GdxGameActions actions = new GdxGameActions(runningMatch(), () -> called.add(true), m -> { });
    actions.requestExit();
    assertEquals(List.of(true), called);
  }

  private static GdxGameActions actionsFor(GameManager match) {
    return new GdxGameActions(match, () -> { }, m -> { });
  }
}
