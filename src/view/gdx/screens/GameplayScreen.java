package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.game.minigame.SpecialStageRule;
import model.game.plant.PlantParts.PlantTemplate;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import view.gdx.core.FixedStepClock;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.input.GameplayInputHandler;
import view.gdx.input.GdxGameActions;
import view.gdx.render.CursorRenderer;
import view.gdx.render.EntityRenderer;
import view.gdx.render.LawnGeometry;
import view.gdx.render.LawnRenderer;
import view.gdx.ui.HudStage;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;

/**
 * The in-match screen.
 *
 * <p>The match itself is the same GameManager the terminal build runs, stepped through
 * GamePlayController so planting rules, waves, win/loss and the end-of-match rewards are not
 * duplicated here.
 *
 * <p>Two clocks on purpose: FixedStepClock runs the simulation at the model's tick rate, and the
 * frame delta is only for drawing.
 *
 * <p>Pausing stops both: no tick, and the renderers get a delta of zero so animations freeze
 * too. Only the HUD keeps acting, since the pause menu lives in it.
 */
public final class GameplayScreen extends BaseScreen {

  private final GameManager match;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);
  private final LawnGeometry geometry;

  private LawnRenderer lawnRenderer;
  private EntityRenderer entityRenderer;
  private CursorRenderer cursorRenderer;
  private HudStage hud;
  private GameplayInputHandler input;
  private GdxGameActions actions;
  private boolean ended;
  private boolean paused;

  public GameplayScreen(PvzGdxGame game, GameManager match) {
    super(game);
    this.match = match;
    this.geometry = new LawnGeometry(boardRows(match), boardColumns(match));
  }

  @Override
  public void show() {
    lawnRenderer = new LawnRenderer(geometry);
    entityRenderer = new EntityRenderer(geometry);

    hud = new HudStage();
    hud.build(game.getUiSkin(), this::leave);

    // refusals go to the HUD toast, not a console the player never sees
    actions = new GdxGameActions(match, this::leave, hud::toast);
    input = new GameplayInputHandler(context(), geometry, actions, this::togglePause);
    cursorRenderer = new CursorRenderer(geometry, input);

    List<PlantTemplate> deck = deckTemplates();
    hud.buildSeedBar(game.getUiSkin().get(), deck, input::setSelectedPlantType,
        input::toggleShovel, input::togglePlantFood, this::togglePause);
    input.setSeedOrder(deckNames(deck));
    clock.reset();

    Gdx.input.setInputProcessor(new InputMultiplexer(hud.getStage(), input));
    layout();
    showBriefing();
  }

  private static List<PlantTemplate> deckTemplates() {
    List<PlantTemplate> templates = new ArrayList<>();
    if (GameDataManager.plantRepository == null) {
      return templates;
    }
    for (String name : MatchSetup.getInstance().getSelectedPlants()) {
      PlantTemplate template = GameDataManager.plantRepository.find(name);
      if (template != null) {
        templates.add(template);
      }
    }
    return templates;
  }

  /** The same order the seed bar draws, so key 1 arms the leftmost card. */
  private static List<String> deckNames(List<PlantTemplate> templates) {
    List<String> names = new ArrayList<>();
    for (PlantTemplate template : templates) {
      names.add(template.name);
    }
    return names;
  }

  private static int plantLevel(String plant) {
    User user = UserManager.getInstance().getCurrentUser();
    return user == null ? 1 : Math.max(1, user.getPlantLevel(plant));
  }

  private void leave() {
    GameSession.end();
    game.switchScreen(new AdventureScreen(game));
  }

  /** Save & Exit: keep whatever the match already awarded, then back out. */
  private void saveAndLeave() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      // worth saying, but must not trap the player in the match
      hud.toast(e.getMessage() == null ? "could not save your progress" : e.getMessage());
    }
    leave();
  }

  /** Rebuilds the level from the same MatchSetup. */
  private void restart() {
    GameSession.end();
    MatchLauncher.launch();
    GameManager restarted = GameSession.getActiveGame();
    if (restarted == null) {
      leave();
      return;
    }
    game.switchScreen(new GameplayScreen(game, restarted));
  }

  private void togglePause() {
    if (ended) {
      return;
    }
    paused = !paused;
    input.setPaused(paused);
    if (paused) {
      hud.showPauseMenu(this::togglePause, this::restart, this::saveAndLeave);
    } else {
      // do not let the match catch up on time spent in the menu
      clock.reset();
    }
  }

  /** Shown once when the level starts: what to expect this level, per the Phase 2 spec. */
  private void showBriefing() {
    if (match == null || game.getUiSkin().get() == null) {
      return;
    }
    Label text = new Label(briefingText(), game.getUiSkin().get(), UiSkinProvider.LABEL_MEDIUM);
    text.setWrap(true);
    Table body = new Table();
    body.add(text).width(420f);
    Popup.show(hud.getStage(), game.getUiSkin().get(), "Level start", body,
        new Popup.Choice("Let's go", UiSkinProvider.BUTTON_GREEN, null));
  }

  private String briefingText() {
    StringBuilder text = new StringBuilder();
    text.append(match.getTotalWaves()).append(" waves of zombies incoming.\n");
    if (match.getBoard() != null && match.getBoard().getGameState().isSkySunDisabled()) {
      text.append("No sun will fall from the sky -- grow your own.\n");
    }
    SpecialStageRule rule = match.getSpecialStageRule();
    if (rule != null) {
      text.append(rule.getClass().getSimpleName()).append(" is active this level.\n");
    }
    text.append("Defend your lawn -- don't let the zombies reach the house!");
    return text.toString();
  }

  private void layout() {
    float[] box = LawnRenderer.lawnBounds(match);
    geometry.setBounds(box[0], box[1], box[2], box[3]);
  }

  @Override
  public void render(float delta) {
    if (!paused && match != null && match.isRunning()) {
      clock.update(delta, actions::advanceOneTick);
    }
    // renderers keep their own animation clocks, so freezing means giving them no time
    float worldDelta = paused ? 0f : delta;

    input.updateHover(Gdx.input.getX(), Gdx.input.getY());

    context().applyCamera();
    lawnRenderer.render(context(), match, worldDelta);
    entityRenderer.render(context(), match, worldDelta);
    if (!paused) {
      cursorRenderer.render(context(), match, worldDelta);
    }

    updateStatus();
    hud.updateSeeds(match, input.getSelectedPlantType(), GameplayScreen::plantLevel);
    hud.updateTools(input.getTool() == GameplayInputHandler.Tool.SHOVEL,
        input.getTool() == GameplayInputHandler.Tool.PLANT_FOOD,
        match == null ? 0 : match.getPlantFoodCount());
    if (match != null && !match.isRunning()) {
      showResult();
    }

    hud.act(delta);
    hud.draw();
  }

  private void updateStatus() {
    if (match == null) {
      hud.setStatus("no match running");
      return;
    }
    if (paused) {
      hud.setStatus("paused");
      return;
    }
    hud.setStatus("wave " + Math.min(match.getCurrentWaveIndex() + 1, match.getTotalWaves())
        + "/" + match.getTotalWaves()
        + "   plant food " + match.getPlantFoodCount()
        + nightNote()
        + hint());
  }

  /** What the next click will do. */
  private String hint() {
    switch (input.getTool()) {
      case SEED:
        return "   placing: " + input.getSelectedPlantType();
      case SHOVEL:
        return "   shovel armed - click a plant to dig it up";
      case PLANT_FOOD:
        return "   plant food armed - click a plant to feed it";
      default:
        return "   pick a seed, then click the lawn";
    }
  }

  /**
   * Dark Ages is at night and nothing falls out of the sky, so every sun has to come off a plant.
   * The background says "night" on its own but not "and that is why your sun is not going up".
   */
  private String nightNote() {
    return match.getBoard() != null && match.getBoard().getGameState().isSkySunDisabled()
        ? "   night: no sun falls from the sky"
        : "";
  }

  private void showResult() {
    if (ended) {
      return;
    }
    ended = true;
    paused = false;
    if (game.getUiSkin().get() == null) {
      return;
    }
    boolean won = match.getMatchResult() != null && match.getMatchResult().isWon();
    Table body = new Table();
    body.add(new Label(won ? "Level cleared!" : "The zombies ate your brains!",
        game.getUiSkin().get(), UiSkinProvider.LABEL_MEDIUM));
    // nothing to cancel once the match is over, so leaving is the only way out
    Popup.show(hud.getStage(), game.getUiSkin().get(), won ? "You win" : "You lose", body,
        "Back to map", this::leave, null, null);
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    hud.resize(width, height);
    layout();
  }

  @Override
  public void hide() {
    Gdx.input.setInputProcessor(null);
  }

  @Override
  public void dispose() {
    if (lawnRenderer != null) {
      lawnRenderer.dispose();
    }
    if (entityRenderer != null) {
      entityRenderer.dispose();
    }
    if (cursorRenderer != null) {
      cursorRenderer.dispose();
    }
    if (hud != null) {
      hud.dispose();
    }
  }

  private static int boardRows(GameManager match) {
    return match != null && match.getBoard() != null
        ? match.getBoard().getRows()
        : GdxConfig.LAWN_ROWS;
  }

  private static int boardColumns(GameManager match) {
    return match != null && match.getBoard() != null
        ? match.getBoard().getColumns()
        : GdxConfig.LAWN_COLUMNS;
  }
}
