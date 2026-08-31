package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import model.core.GameManager;
import model.game.minigame.ConveyorRule;
import model.game.zombie.behavior.ZombossHealth;
import view.gdx.core.GdxConfig;
import model.game.plant.PlantParts.PlantTemplate;
import view.gdx.core.GdxConfig;

/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, the shovel and plant food tools, the
 * pause button and the pause menu itself.
 *
 * <p>Its own FitViewport, but on the same virtual size as the world. A ScreenViewport here meant
 * one unit per real pixel, so at 1440p the HUD kept its pixel size and shrank to half the screen
 * while the lawn scaled up past it; matching the world's letterbox keeps the two in step and the
 * seed bar over the lane it belongs to at any window size.
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
  private static final float TOOL_BUTTON_WIDTH = 62f;
  // Package-private, not private: HudToolsRowHeightTest checks these against SeedBar.CARD_HEIGHT
  // without needing a live Scene2D layout to do it.
  static final float TOOL_BUTTON_HEIGHT = 62f;
  static final float TOOL_BUTTON_PAD = 3f;

  /**
   * How much of the window's height the HUD may occupy before it starts covering the top lane.
   *
   * <p>The lawn's top edge is where the world art puts it (see
   * {@link view.gdx.render.SeasonBackdrop}), which on every season is a little under a quarter of
   * the way down. Anything the HUD stacks above that has to fit in this, which is why the
   * objective and status lines moved to the foot of the screen: they are the two pieces that grow
   * with their text, and up here that growth lands on the lawn.
   */
  private static final float TOP_BLOCK_LIMIT = 190f;

  private final Stage stage;
  private final HudArt hudArt = new HudArt();

  private Skin skin;
  private Label sunCount;
  private Label status;
  private Label objective;

  private Table waveSlot;
  private WaveBar waveBar;
  private BossBar bossBar;
  private Table seedBar;
  private Table extras;
  private Table footer;
  private SeedBar seeds;
  private ConveyorBar conveyor;

  private Button shovelButton;
  private Button plantFoodButton;
  private Label plantFoodCount;
  private Button startWavesButton;

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

    // The lawn starts just below, so keep this block short. See TOP_BLOCK_LIMIT.
    Table topRow = new Table();
    topRow.add(counters(skin)).left();
    waveSlot = new Table();
    topRow.add(waveSlot).left().padLeft(18f).expandX();
    if (onExit != null) {
      topRow.add(exitButton(skin, onExit)).right().width(112f).height(42f);
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

  /** داک: در مرحلهٔ باس، به‌جای نوار موج‌ها نوار جانِ سه‌تکهٔ زامباس نشان داده می‌شود. */
  public void buildBossBar(String bossName) {
    if (waveSlot == null || skin == null) {
      return;
    }
    waveBar = null;
    bossBar = new BossBar(skin, bossName);
    waveSlot.clear();
    waveSlot.add(bossBar);
  }

  public void updateBoss(ZombossHealth health, int currentHealth, boolean stunned) {
    if (bossBar != null) {
      bossBar.update(health, currentHealth, stunned);
    }
  }

  public void setObjective(String text) {
    if (objective != null) {
      objective.setText(text == null ? "" : text);
      refreshFooter();
    }
  }

  /** The red warning: a wave landing, necromancy, the tide turning. */
  public void alert(String message) {
    Toast.showAlert(stage, skin, message);
  }

  /**
   * Coins, gems and sun on one dark plate.
   *
   * <p>They used to sit straight on the world art, which is sand in one chapter and ice in
   * another, so the one row of numbers the player checks constantly had a different amount of
   * contrast in every level and none at all over the bright patches. The plate is the original
   * game's own -- see {@link HudPlates} -- and costs nothing: it is already in the skin atlas the
   * HUD has loaded.
   */
  private Table counters(Skin skin) {
    Table box = new Table();
    Drawable plate = HudPlates.plate(skin);
    if (plate != null) {
      box.setBackground(plate);
    }
    box.pad(6f, 16f, 6f, 18f);
    box.add(new CurrencyHud(skin)).left().padRight(20f);
    box.add(sunCounter(skin)).left();
    return box;
  }

  /** Sun gets its own icon and a bigger number, it is the one figure the player watches. */
  private Table sunCounter(Skin skin) {
    Table box = new Table();
    // The skin's own HUD sun is drawn for a counter; the lawn's collectible is the fallback.
    Drawable icon = HudPlates.drawable(skin, HudPlates.SUN_ICON);
    if (icon != null) {
      box.add(new Image(icon)).size(36f, 36f).padRight(6f);
    } else {
      TextureRegion sun = hudArt.find("sun");
      if (sun != null) {
        Image fallback = new Image(sun);
        fallback.setScaling(Scaling.fit);
        box.add(fallback).size(34f, 34f).padRight(6f);
      }
    }
    sunCount = new Label("0", skin, UiSkinProvider.LABEL_BIG);
    box.add(sunCount).left().minWidth(56f);
    return box;
  }

  /**
   * The objective and status lines, moved to the foot of the window.
   *
   * <p>Both grow with their text, and at the top of the screen that growth pushed the seed bar
   * down onto the lawn's first lane. Every season paints a dark decorative band along the bottom
   * of its backdrop and nothing is ever played there, so the two lines read better and cost the
   * board nothing.
   */
  private void buildFooter(Skin skin) {
    footer = new Table();
    Drawable plate = HudPlates.plate(skin);
    if (plate != null) {
      footer.setBackground(plate);
    }
    footer.pad(5f, 16f, 6f, 18f);
    objective = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    status = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    footer.add(objective).left().row();
    footer.add(status).left();
    footer.setVisible(false);

    Table anchor = new Table();
    anchor.setFillParent(true);
    anchor.bottom().left().pad(10f);
    anchor.add(footer);
    stage.addActor(anchor);
  }

  /** Hides the plate when there is nothing on it, so an empty bar never floats at the foot. */
  private void refreshFooter() {
    if (footer == null) {
      return;
    }
    boolean anything = (objective != null && objective.getText().length() > 0)
        || (status != null && status.getText().length() > 0);
    footer.setVisible(anything);
  }

  public void setSun(int amount) {
    if (sunCount != null) {
      sunCount.setText(String.valueOf(amount));
    }
  }

  private TextButton exitButton(Skin skin, Runnable onExit) {
    TextButton button = new TextButton("Menu", skin, UiSkinProvider.BUTTON_BROWN);
    ButtonFeel.apply(button);
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
    shovelButton = iconTool(skin, HudPlates.SHOVEL, HudPlates.SHOVEL_ARMED, "Shovel", onShovel);
    plantFoodButton = iconTool(skin, HudPlates.PLANT_FOOD, HudPlates.PLANT_FOOD_ARMED,
        "Food 0", onPlantFood);
    tools.add(shovelButton);
    // The plant food count rides on its own button rather than in its label, so the icon stays
    // an icon. Stacked, so the number sits over the leaf's bottom corner the way PvZ2 puts it.
    plantFoodCount = new Label("0", skin, UiSkinProvider.LABEL_MEDIUM_OUTLINE);
    Table countCorner = new Table();
    // Tucked into the leaf's own bottom-right rather than the cell's, or the number floats off
    // beside the icon and reads as belonging to the pause button next to it.
    countCorner.bottom().right().pad(0f, 0f, 6f, 8f);
    countCorner.add(plantFoodCount);
    tools.add(new Stack(plantFoodButton, countCorner));
    tools.add(iconTool(skin, HudPlates.PAUSE, HudPlates.PAUSE_DOWN, "Pause", onPause));
    return tools;
  }

  /**
   * One tool button, drawn as the original game draws it.
   *
   * <p>The skin carries PvZ2's own shovel, plant-food and pause buttons and the HUD was using
   * brown text buttons instead -- three words where the game has three unmistakable icons, taking
   * nearly twice the width. The {@code _down} art is the lit variant, so an armed tool is the same
   * button glowing rather than a tinted rectangle.
   *
   * <p>Falls back to the old text button whenever the skin has no such region, so a skin without
   * the HUD set still gets a working, labelled control.
   */
  private Button iconTool(Skin skin, String region, String armedRegion, String label,
      Runnable action) {
    Drawable up = HudPlates.drawable(skin, region);
    Drawable armed = HudPlates.drawable(skin, armedRegion);
    Button button;
    if (up == null) {
      button = new TextButton(label, skin, UiSkinProvider.BUTTON_BROWN);
    } else {
      ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
      style.imageUp = up;
      style.imageDown = armed == null ? up : armed;
      style.imageChecked = style.imageDown;
      ImageButton image = new ImageButton(style);
      image.getImage().setScaling(Scaling.fit);
      button = image;
    }
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

  private Button toolButton(Skin skin, String label, Runnable action) {
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
  public void updateTools(boolean shovelArmed, boolean plantFoodArmed, int foodCount) {
    if (shovelButton != null) {
      shovelButton.setChecked(shovelArmed);
      shovelButton.setColor(shovelArmed ? TOOL_ARMED : TOOL_IDLE);
    }
    if (plantFoodButton != null) {
      plantFoodButton.setChecked(plantFoodArmed);
      // Nothing to spend reads as a dimmed button, the same way a seed card does.
      plantFoodButton.setColor(plantFoodArmed || foodCount > 0 ? TOOL_ARMED : TOOL_IDLE);
      if (plantFoodButton instanceof TextButton text) {
        text.setText("Food " + foodCount);
      }
    }
    if (plantFoodCount != null) {
      plantFoodCount.setText(String.valueOf(foodCount));
    }
  }

  public void updateSeeds(GameManager match, String selected, ToIntFunction<String> levelOf) {
    if (seeds != null) {
      seeds.update(match, selected, levelOf);
    }
  }

  public void setStatus(String text) {
    if (status != null) {
      status.setText(text == null ? "" : text);
      refreshFooter();
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
