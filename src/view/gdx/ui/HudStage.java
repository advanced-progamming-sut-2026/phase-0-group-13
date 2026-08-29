package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import model.core.GameManager;
import model.game.minigame.ConveyorRule;
import view.gdx.core.GdxConfig;
import model.game.plant.PlantParts.PlantTemplate;

/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, the shovel and plant food tools, the
 * pause button and the pause menu itself.
 *
 * <p>Measured in the same 1280x720 world the lawn is drawn in, and letterboxed the same way. The
 * menus use a ScreenViewport instead, so their text stays pixel-crisp at any window size, and this
 * used to as well -- but a menu floats over a backdrop while this sits directly on top of the
 * playfield, and the two viewports only agree at exactly 1280x720. Anywhere else they drift: the
 * lawn scales with the window and the HUD does not, so on a smaller window the un-scaled card strip
 * ran off the left edge and cut the first seed card in half, and slid down over the top lane; on a
 * wide one the Menu button walked away from the lawn's right edge. Sharing the world's shape costs
 * some sharpness above the design size -- the same softening the lawn art already takes -- and in
 * exchange the HUD cannot come adrift from the board.
 *
 * <p>Its own FitViewport rather than the world's instance: two FitViewports of the same size map
 * identically, and a separate one keeps the Stage from driving the camera the renderers use.
 *
 * <p>Height is the scarce thing here. Everything stacked above the seed bar comes off the top lane,
 * and the gap is small: the lawn starts 177px down in Frostbite Caves and 187-189px in the other
 * three. The stack measured 203px and covered the top lane in all four before the objective line
 * moved to the footer; it is 174px now. Anything new added above the seed bar has to come out of
 * that budget, so prefer the footer. HudLayoutTest holds the line.
 *
 * <p>Owns no game state. The tool buttons only report a press; what is armed lives in
 * GameplayInputHandler and comes back through {@link #updateTools}, so they cannot disagree with
 * the cursor.
 *
 * <p>build() does nothing while there's no skin, see UiSkinProvider.
 */
public final class HudStage implements Disposable {

  private static final Color TOOL_ARMED = new Color(1f, 1f, 1f, 1f);
  private static final Color TOOL_IDLE = new Color(0.62f, 0.62f, 0.66f, 1f);

  /**
   * The tools sit beside the seed bar in the same row (see buildSeedBar/buildConveyorBar), so this
   * row's height becomes that whole row's height -- three of these stacked (162px) used to stand
   * taller than a seed card (110px, see SeedBar.CARD_HEIGHT) and push the lawn's top row down
   * underneath the HUD. One row of three fixes that: 48 + this pad on both sides comes to 54px,
   * comfortably under a card.
   */
  private static final float TOOL_BUTTON_WIDTH = 112f;
  // Package-private, not private: HudToolsRowHeightTest checks these against SeedBar.CARD_HEIGHT
  // without needing a live Scene2D layout to do it.
  static final float TOOL_BUTTON_HEIGHT = 48f;
  static final float TOOL_BUTTON_PAD = 3f;

  private final Stage stage;
  private final HudArt hudArt = new HudArt();

  private Skin skin;
  private Label sunCount;
  private Label status;
  private Label objective;

  private Table waveSlot;
  private WaveBar waveBar;
  private Table seedBar;
  private Table extras;
  private SeedBar seeds;
  private ConveyorBar conveyor;

  private TextButton shovelButton;
  private TextButton plantFoodButton;
  private TextButton startWavesButton;

  private String selected;

  public HudStage() {
    this.stage = new Stage(new FitViewport(GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT));
  }

  /** The stage itself, for putting in an InputMultiplexer. */
  public Stage getStage() {
    return stage;
  }

  /**
   * Fills in the HUD. Does nothing without a skin, so a screen can just call it.
   *
   * @param onExit what the Menu button does, or null to leave it out
   */
  public void build(UiSkinProvider skinProvider, Runnable onExit, GameManager match) {
    if (!skinProvider.isAvailable()) {
      return;
    }
    this.skin = skinProvider.get();

    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(4f);
    stage.addActor(root);

    // The lawn starts just below, so keep this block short.
    Table topRow = new Table();
    topRow.add(new CurrencyHud(skin)).left();
    topRow.add(sunCounter(skin)).left().padLeft(22f);
    waveSlot = new Table();
    topRow.add(waveSlot).left().padLeft(22f);
    status = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    topRow.add(status).left().padLeft(22f).expandX();
    if (onExit != null) {
      topRow.add(exitButton(skin, onExit)).right().width(120f).height(46f);
    }
    root.add(topRow).growX().row();

    seedBar = new Table();
    root.add(seedBar).left().padTop(2f).row();

    extras = new Table();
    root.add(extras).left().padTop(4f).row();

    // What this stage wants from the player, when it wants something unusual.
    //
    // Along the bottom rather than under the seed bar. Everything the HUD stacks above the lawn
    // comes straight off the top lane -- the seed bar already reaches 203px down against a lawn
    // that starts at 177px in Frostbite Caves -- and this is the one line of it that does not have
    // to be next to the seeds. The strip under the board is empty in every season.
    Table footer = new Table();
    footer.setFillParent(true);
    footer.bottom().left().pad(6f);
    objective = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    footer.add(objective).left();
    stage.addActor(footer);

    if (DebugPanel.isEnabled()) {
      // Out of the way in the corner - the HUD flows from the top and the lawn is under it.
      Table corner = new Table();
      corner.setFillParent(true);
      corner.bottom().right().pad(8f);
      corner.add(new DebugPanel(skin, this::toast, match));
      stage.addActor(corner);
    }
  }

  /** One marker per wave. Needs the match's wave count, so it comes after build(). */
  public void buildWaveBar(int totalWaves) {
    if (waveSlot == null || skin == null) {
      return;
    }
    waveBar = new WaveBar(skin, totalWaves);
    waveSlot.clear();
    waveSlot.add(waveBar);
  }

  public void updateWave(int currentWaveIndex) {
    if (waveBar != null) {
      waveBar.update(currentWaveIndex);
    }
  }

  public void setObjective(String text) {
    if (objective != null) {
      objective.setText(text == null ? "" : text);
    }
  }

  /** The red warning: a wave landing, necromancy, the tide turning. */
  public void alert(String message) {
    Toast.showAlert(stage, skin, message);
  }

  /** Sun gets its own icon and a bigger number, it is the one figure the player watches. */
  private Table sunCounter(Skin skin) {
    Table box = new Table();
    TextureRegion sun = hudArt.find("sun");
    if (sun != null) {
      Image icon = new Image(sun);
      icon.setScaling(Scaling.fit);
      box.add(icon).size(34f, 34f).padRight(6f);
    }
    sunCount = new Label("0", skin, UiSkinProvider.LABEL_BIG);
    box.add(sunCount).left();
    return box;
  }

  public void setSun(int amount) {
    if (sunCount != null) {
      sunCount.setText(String.valueOf(amount));
    }
  }

  private TextButton exitButton(Skin skin, Runnable onExit) {
    TextButton button = new TextButton("Menu", skin, UiSkinProvider.BUTTON_BROWN);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            onExit.run();
          }
        });
    return button;
  }

  // Seeds and tools share a row: the lawn starts right underneath and a second row would cover
  // the top lane.
  public void buildSeedBar(Skin skin, List<PlantTemplate> templates, Consumer<String> onPick,
      Runnable onShovel, Runnable onPlantFood, Runnable onPause) {
    if (seedBar == null || skin == null) {
      return;
    }
    seedBar.clear();
    seeds = new SeedBar(skin, templates, plant -> {
      selected = plant;
      onPick.accept(plant);
    });
    seedBar.add(seeds).left();
    seedBar.add(toolsRow(skin, onShovel, onPlantFood, onPause)).left().padLeft(10f).top();
  }

  /** Conveyor Belt stages have no seed bank to draw: the belt is the bar. */
  public void buildConveyorBar(ConveyorRule rule, Consumer<String> onPick, Runnable onShovel,
      Runnable onPlantFood, Runnable onPause) {
    if (seedBar == null || skin == null) {
      return;
    }
    seedBar.clear();
    conveyor = new ConveyorBar(skin, rule, plant -> {
      selected = plant;
      onPick.accept(plant);
    });
    seedBar.add(conveyor).left();
    seedBar.add(toolsRow(skin, onShovel, onPlantFood, onPause)).left().padLeft(10f).top();
  }

  public void updateConveyor() {
    if (conveyor != null) {
      conveyor.update();
    }
  }

  /** The free-build stages: plant what you like, then let the zombies in. */
  public void buildStartWavesButton(Runnable onStart) {
    if (extras == null || skin == null) {
      return;
    }
    startWavesButton = toolButton(skin, "Start the waves", onStart);
    extras.add(startWavesButton).width(230f).height(48f);
  }

  public void setStartWavesVisible(boolean visible) {
    if (startWavesButton != null) {
      startWavesButton.setVisible(visible);
    }
  }

  /**
   * Shovel/Food/Pause side by side rather than stacked -- see the class-level comment on
   * TOOL_BUTTON_HEIGHT for why a stack was the wrong shape here.
   */
  private Table toolsRow(Skin skin, Runnable onShovel, Runnable onPlantFood, Runnable onPause) {
    Table tools = new Table();
    tools.defaults().pad(TOOL_BUTTON_PAD).width(TOOL_BUTTON_WIDTH).height(TOOL_BUTTON_HEIGHT);
    shovelButton = toolButton(skin, "Shovel", onShovel);
    plantFoodButton = toolButton(skin, "Food 0", onPlantFood);
    tools.add(shovelButton);
    tools.add(plantFoodButton);
    tools.add(toolButton(skin, "Pause", onPause));
    return tools;
  }

  private TextButton toolButton(Skin skin, String label, Runnable action) {
    TextButton button = new TextButton(label, skin, UiSkinProvider.BUTTON_BROWN);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (action != null) {
              action.run();
            }
          }
        });
    return button;
  }

  /** Repaints the tool buttons from what the input handler has armed. */
  public void updateTools(boolean shovelArmed, boolean plantFoodArmed, int plantFoodCount) {
    if (shovelButton != null) {
      shovelButton.setColor(shovelArmed ? TOOL_ARMED : TOOL_IDLE);
    }
    if (plantFoodButton != null) {
      plantFoodButton.setColor(plantFoodArmed ? TOOL_ARMED : TOOL_IDLE);
      plantFoodButton.setText("Food " + plantFoodCount);
    }
  }

  public void updateSeeds(GameManager match, String selected, ToIntFunction<String> levelOf) {
    if (seeds != null) {
      seeds.update(match, selected, levelOf);
    }
  }

  public void setStatus(String text) {
    if (status != null) {
      status.setText(text);
    }
  }

  public String getSelected() {
    return selected;
  }

  /** In-match message, for a refused plant or a collected reward. */
  public void toast(String message) {
    Toast.show(stage, skin, message);
  }

  /** Modal, so nothing underneath is clickable while the game is frozen. */
  public void showPauseMenu(Runnable onResume, Runnable onRestart, Runnable onSaveExit) {
    Popup.show(stage, skin, "Paused", null,
        new Popup.Choice("Resume", UiSkinProvider.BUTTON_GREEN, onResume),
        new Popup.Choice("Restart", UiSkinProvider.BUTTON_PURPLE, onRestart),
        new Popup.Choice("Save & Exit", UiSkinProvider.BUTTON_BROWN, onSaveExit));
  }

  public void act(float delta) {
    stage.act(delta);
  }

  public void draw() {
    stage.getViewport().apply();
    stage.draw();
  }

  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    if (seeds != null) {
      seeds.dispose();
    }
    if (conveyor != null) {
      conveyor.dispose();
    }
    hudArt.dispose();
    stage.dispose();
  }
}
