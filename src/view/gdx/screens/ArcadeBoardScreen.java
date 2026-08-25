package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import data.persistence.UserManager;
import model.account.User;
import model.core.MiniGameLauncher;
import model.enums.MiniGameType;
import view.gdx.core.FixedStepClock;
import view.gdx.core.GameSettings;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.ArcadeRenderer;
import view.gdx.render.LawnGeometry;
import view.gdx.render.LawnRenderer;
import view.gdx.ui.Popup;
import view.gdx.ui.Toast;
import view.gdx.ui.UiSkinProvider;

/**
 * What the three arcade mini-games have in common: a lawn, a clock and a way out.
 *
 * <p>Each of them runs a Phase One engine over a five-by-nine board, so the parts that are the
 * same are the parts that are not about the game -- the season backdrop and its measured lawn
 * bounds, stepping the engine at the model's own tick rate, turning a click into a cell, pausing,
 * and awarding the clear through {@link MiniGameLauncher}. What is left to a subclass is the two
 * things that differ: what to draw and what a click means.
 *
 * <p>Two clocks, same as {@link GameplayScreen}: {@link FixedStepClock} steps the engine and the
 * frame delta only drives animation. Pausing gives the renderers a delta of zero so the rigs
 * freeze with the simulation instead of walking on the spot.
 */
public abstract class ArcadeBoardScreen extends BaseScreen {

  protected static final int ROWS = 5;
  protected static final int COLUMNS = 9;

  /** Gap between the lawn's left edge and anything standing beside it. */
  protected static final float LANE_PROP_GAP = 8f;

  /** Below the picker strip, so a refused click is not written behind a row of cards. */
  private static final float MESSAGE_TOP_PADDING = 200f;

  private static final Color LIGHT_LANE = new Color(1f, 1f, 1f, 0.10f);
  private static final Color DARK_LANE = new Color(0f, 0f, 0f, 0.10f);
  private static final Color HOVER = new Color(1f, 1f, 1f, 0.22f);

  private final MiniGameType type;
  private final int level;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);
  private final Vector2 pointer = new Vector2();

  protected final LawnGeometry geometry = new LawnGeometry(ROWS, COLUMNS);
  protected ArcadeRenderer art;

  private LawnRenderer lawn;
  private Stage stage;
  private Skin skin;
  private Table picker;
  private Label status;
  private boolean paused;
  private boolean ended;
  private int hoverRow = -1;
  private int hoverColumn = -1;

  protected ArcadeBoardScreen(PvzGdxGame game, MiniGameType type, int level) {
    super(game);
    this.type = type;
    this.level = level;
  }

  /** Which season's lawn this mini-game is played on. See {@link LawnRenderer#lawnBounds}. */
  protected abstract String seasonKey();

  /** Shown top left, above the lawn. */
  protected abstract String title();

  /** The line of resources and objectives under the title, redrawn every frame. */
  protected abstract String statusLine();

  /** Fills the strip beside the title: seed packets, a belt, a zombie picker. */
  protected abstract void buildPicker(Table picker, Skin skin);

  /** Called after every engine tick, for a picker whose contents change with the game. */
  protected void refreshPicker() {}

  /** Draws the board. The batch is already begun and the camera already applied. */
  protected abstract void drawWorld(float delta);

  /** Filled shapes over the board: health bars, the red line, markers. */
  protected void drawOverlays(ShapeRenderer shapes) {}

  /** Outlines, for an entity with no verified art. Drawn after the filled pass. */
  protected void drawOutlines(ShapeRenderer shapes) {}

  /** One engine tick. */
  protected abstract void tickEngine();

  protected abstract boolean engineFinished();

  protected abstract boolean engineWon();

  /** A click on a lawn cell. Return a message to toast, or null to say nothing. */
  protected abstract String onCellClicked(int row, int column);

  @Override
  public void show() {
    lawn = new LawnRenderer(geometry);
    art = new ArcadeRenderer(geometry);
    skin = game.getUiSkin().get();

    stage = new Stage(new ScreenViewport());
    if (skin != null) {
      buildHud();
    }
    Gdx.input.setInputProcessor(new InputMultiplexer(stage, new LawnInput()));
    layout();
    clock.reset();
  }

  private void buildHud() {
    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(6f);
    stage.addActor(root);

    Table header = new Table();
    header.add(new Label(title(), skin, UiSkinProvider.LABEL_BIG)).left().padRight(24f);
    status = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    header.add(status).left().expandX();
    if (canPause()) {
      header.add(barButton("Pause", this::togglePause)).width(120f).height(44f).padRight(6f);
    }
    header.add(barButton(leaveLabel(), this::leave)).width(140f).height(44f);
    root.add(header).growX().row();

    picker = new Table();
    picker.left();
    root.add(picker).left().padTop(4f).padLeft(6f).row();
    buildPicker(picker, skin);
  }

  private TextButton barButton(String label, Runnable action) {
    TextButton button = new TextButton(label, skin, UiSkinProvider.BUTTON_BROWN);
    button.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        action.run();
      }
    });
    return button;
  }

  /** Rebuilds the picker strip from scratch. Subclasses call it when their contents change. */
  protected final void rebuildPicker() {
    if (picker == null || skin == null) {
      return;
    }
    picker.clear();
    buildPicker(picker, skin);
  }

  protected final void toast(String message) {
    if (message != null && !message.isBlank()) {
      Toast.show(stage, skin, message, MESSAGE_TOP_PADDING);
    }
  }

  protected final boolean isPaused() {
    return paused;
  }

  protected final int getLevel() {
    return level;
  }

  protected final int hoverRow() {
    return hoverRow;
  }

  protected final int hoverColumn() {
    return hoverColumn;
  }

  private void layout() {
    float[] box = LawnRenderer.lawnBounds(seasonKey());
    geometry.setBounds(box[0], box[1], box[2], box[3]);
  }

  @Override
  public void render(float delta) {
    if (!paused && !ended) {
      clock.update(delta * GameSettings.getGameSpeed(), this::step);
    }
    float worldDelta = paused ? 0f : delta;

    context().applyCamera();
    lawn.renderBackdrop(context(), seasonKey(), worldDelta);
    drawLanes();

    art.beginFrame(worldDelta);
    context().getBatch().begin();
    drawWorld(worldDelta);
    context().getBatch().end();
    art.endFrame();

    Gdx.gl.glEnable(GL20.GL_BLEND);
    ShapeRenderer shapes = context().getShapes();
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    drawOverlays(shapes);
    shapes.end();
    shapes.begin(ShapeRenderer.ShapeType.Line);
    drawOutlines(shapes);
    shapes.end();

    if (status != null) {
      status.setText(paused ? "paused  -  " + statusLine() : statusLine());
    }
    if (!ended && engineFinished()) {
      finish();
    }
    stage.act(delta);
    stage.getViewport().apply();
    stage.draw();
  }

  private void step() {
    tickEngine();
    refreshPicker();
  }

  /** The lane grid and the cell under the mouse, so a click has something to aim at. */
  private void drawLanes() {
    ShapeRenderer shapes = context().getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        boolean hovered = row == hoverRow && col == hoverColumn;
        if (!hovered && !GameSettings.isGridVisible()) {
          continue;
        }
        shapes.setColor(hovered ? HOVER : (row + col) % 2 == 0 ? LIGHT_LANE : DARK_LANE);
        shapes.rect(geometry.columnToX(col) + 1f, geometry.rowToY(row) + 1f,
            geometry.getCellWidth() - 2f, geometry.getCellHeight() - 2f);
      }
    }
    shapes.end();
  }

  /** The red line neither Bowling nor I, Zombie lets the player plant past. */
  protected final void drawRedLine(ShapeRenderer shapes, int column, boolean onTheRight) {
    shapes.setColor(0.85f, 0.15f, 0.15f, 0.75f);
    float x = onTheRight ? geometry.columnToX(column + 1) : geometry.columnToX(column);
    shapes.rect(x - 2f, geometry.rowToY(ROWS - 1), 4f, geometry.getCellHeight() * ROWS);
  }

  /** False for a match whose clock is somewhere else, which nothing here may stop. */
  protected boolean canPause() {
    return true;
  }

  protected String leaveLabel() {
    return "Give up";
  }

  private void togglePause() {
    if (ended || !canPause()) {
      return;
    }
    paused = !paused;
    if (paused) {
      Popup.show(stage, skin, "Paused", null,
          new Popup.Choice("Resume", UiSkinProvider.BUTTON_GREEN, this::togglePause),
          new Popup.Choice("Give up", UiSkinProvider.BUTTON_BROWN, this::leave));
    } else {
      clock.reset();
    }
  }

  private void finish() {
    ended = true;
    paused = false;
    boolean won = engineWon();
    recordOutcome(won);
    if (skin == null) {
      return;
    }
    Table body = new Table();
    body.add(new Label(won ? outcomeWon() : outcomeLost(), skin, UiSkinProvider.LABEL_MEDIUM));
    Popup.show(stage, skin, won ? "You win" : "You lose", body,
        leaveButtonLabel(), this::leave, null, null);
  }

  /** The clear is recorded by the same Phase One path the terminal build uses. */
  protected void recordOutcome(boolean won) {
    User user = UserManager.getInstance().getCurrentUser();
    if (!won || user == null) {
      return;
    }
    MiniGameLauncher.awardClear(user, type, level);
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      toast(e.getMessage() == null ? "could not save your progress" : e.getMessage());
    }
  }

  protected String leaveButtonLabel() {
    return "Back to mini-games";
  }

  protected String outcomeWon() {
    return "Mini-game cleared!";
  }

  protected String outcomeLost() {
    return "The zombies won this one.";
  }

  protected void leave() {
    game.switchScreen(new MiniGamesScreen(game));
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    stage.getViewport().update(width, height, true);
    layout();
  }

  @Override
  public void hide() {
    Gdx.input.setInputProcessor(null);
  }

  @Override
  public void dispose() {
    if (lawn != null) {
      lawn.dispose();
    }
    if (art != null) {
      art.dispose();
    }
    if (stage != null) {
      stage.dispose();
    }
  }

  /** Under the HUD in the multiplexer, so a button press never also lands on the lawn. */
  private final class LawnInput extends InputAdapter {

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
      hover(screenX, screenY);
      return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointerIndex) {
      hover(screenX, screenY);
      return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointerIndex, int button) {
      hover(screenX, screenY);
      if (paused || ended || hoverRow < 0 || hoverColumn < 0) {
        return false;
      }
      toast(onCellClicked(hoverRow, hoverColumn));
      return true;
    }

    @Override
    public boolean keyDown(int keycode) {
      if (keycode == Input.Keys.ESCAPE) {
        leave();
        return true;
      }
      if (keycode == Input.Keys.SPACE) {
        togglePause();
        return true;
      }
      return false;
    }

    private void hover(int screenX, int screenY) {
      pointer.set(screenX, screenY);
      context().getViewport().unproject(pointer);
      hoverRow = geometry.yToRow(pointer.y);
      hoverColumn = geometry.xToColumn(pointer.x);
    }
  }
}
