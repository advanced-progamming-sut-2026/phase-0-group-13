package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import model.core.GameManager;
import view.gdx.core.FixedStepClock;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.input.GameActionBridge;
import view.gdx.input.GameplayInputHandler;
import view.gdx.render.EntityRenderer;
import view.gdx.render.LawnGeometry;
import view.gdx.render.LawnRenderer;
import view.gdx.ui.HudStage;


/**
 * The in-match screen, where the rest of the graphical layer comes together.
 *
 * <p>Takes the GameManager instead of reading model.core.GameSession, so it doesn't depend on the
 * terminal flow's static state and can be opened from anywhere. Null is fine, it just draws an
 * empty lawn, which is what happens at the moment.
 *
 * <p>Two clocks on purpose: FixedStepClock runs the simulation at the model's tick rate, and the
 * frame delta is only for animation. If we mixed them the game speed would follow the frame rate.
 *
 * <p>Nothing is playable yet. The simulation step is still a TODO because the model prints to
 * System.out and a screen can't catch that.
 */
public final class GameplayScreen extends BaseScreen {

  private final GameManager match;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);

  private LawnGeometry geometry;
  private LawnRenderer lawnRenderer;
  private EntityRenderer entityRenderer;
  private HudStage hud;
  private GameplayInputHandler input;

  /**
   * @param match the match to draw, or null for an empty lawn
   * @param actions where input goes, can be null until there's an adapter
   */
  public GameplayScreen(PvzGdxGame game, GameManager match, GameActionBridge actions) {
    super(game);
    this.match = match;
    this.geometry = new LawnGeometry(boardRows(match), boardColumns(match));
    this.input = new GameplayInputHandler(context(), geometry, actions);
  }

  @Override
  public void show() {
    lawnRenderer = new LawnRenderer(geometry);
    entityRenderer = new EntityRenderer(geometry);
    hud = new HudStage();
    hud.build(game.getUiSkin());
    clock.reset();

    // HUD first so buttons get the click, anything else goes to the lawn
    InputMultiplexer multiplexer = new InputMultiplexer(hud.getStage(), input);
    Gdx.input.setInputProcessor(multiplexer);

    layout();
  }

  /** Puts the lawn somewhere in the world. Called on show and on every resize. */
  private void layout() {
    // TODO leave room for the seed bar and the HUD once we know how big they are
    geometry.setBounds(0f, 0f, GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT);
  }

  @Override
  public void render(float delta) {
    // TODO once the model stops printing to System.out, run the simulation here:
    //   clock.update(delta, match::advanceTime);
    // until then only the terminal version moves the match forward

    context().applyCamera();
    lawnRenderer.render(context(), match, delta);
    entityRenderer.render(context(), match, delta);

    hud.act(delta);
    hud.draw();
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
