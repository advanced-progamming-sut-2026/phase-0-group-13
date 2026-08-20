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
import model.game.plant.PlantParts.PlantTemplate;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchSetup;
import view.gdx.core.FixedStepClock;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.input.GameActionBridge;
import view.gdx.input.GameplayInputHandler;
import view.gdx.input.GdxGameActions;
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
 */
public final class GameplayScreen extends BaseScreen {

  private final GameManager match;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);
  private final LawnGeometry geometry;

  private LawnRenderer lawnRenderer;
  private EntityRenderer entityRenderer;
  private HudStage hud;
  private GameplayInputHandler input;
  private GdxGameActions actions;
  private boolean ended;

  public GameplayScreen(PvzGdxGame game, GameManager match, GameActionBridge unused) {
    super(game);
    this.match = match;
    this.geometry = new LawnGeometry(boardRows(match), boardColumns(match));
  }

  @Override
  public void show() {
    lawnRenderer = new LawnRenderer(geometry);
    entityRenderer = new EntityRenderer(geometry);
    actions = new GdxGameActions(match, this::leave);
    input = new GameplayInputHandler(context(), geometry, actions);

    hud = new HudStage();
    hud.build(game.getUiSkin(), this::leave);
    hud.buildSeedBar(game.getUiSkin().get(), deckTemplates(), input::setSelectedPlantType);
    clock.reset();

    Gdx.input.setInputProcessor(new InputMultiplexer(hud.getStage(), input));
    layout();
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

  private static int plantLevel(String plant) {
    User user = UserManager.getInstance().getCurrentUser();
    return user == null ? 1 : Math.max(1, user.getPlantLevel(plant));
  }

  private void leave() {
    GameSession.end();
    game.switchScreen(new AdventureScreen(game));
  }

  private void layout() {
    float[] box = LawnRenderer.lawnBounds(match);
    geometry.setBounds(box[0], box[1], box[2], box[3]);
  }

  @Override
  public void render(float delta) {
    if (match != null && match.isRunning()) {
      clock.update(delta, actions::advanceOneTick);
    }

    context().applyCamera();
    lawnRenderer.render(context(), match, delta);
    entityRenderer.render(context(), match, delta);

    updateStatus();
    hud.updateSeeds(match, input.getSelectedPlantType(), GameplayScreen::plantLevel);
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
    String picked = input.getSelectedPlantType();
    hud.setSun(match.getSunAmount());
    hud.setStatus("wave " + Math.min(match.getCurrentWaveIndex() + 1, match.getTotalWaves())
        + "/" + match.getTotalWaves()
        + "   plant food " + match.getPlantFoodCount()
        + (picked == null ? "   pick a seed, then click the lawn" : "   placing: " + picked));
  }

  private void showResult() {
    if (ended) {
      return;
    }
    ended = true;
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
