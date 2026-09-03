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
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import model.core.GameManager;
import model.game.minigame.ConveyorRule;
import model.game.zombie.behavior.ZombossHealth;
import view.gdx.core.GdxConfig;
import model.game.plant.PlantParts.PlantTemplate;
import view.gdx.core.GdxConfig;

public final class HudStage implements Disposable {

  private static final Color TOOL_ARMED = new Color(1f, 1f, 1f, 1f);
  private static final Color TOOL_IDLE = new Color(0.62f, 0.62f, 0.66f, 1f);

  /** One row beside the seed bar; must stay under SeedBar.CARD_HEIGHT or it covers the top lane. */
  private static final float TOOL_BUTTON_WIDTH = 62f;
  // Package-private, not private: HudToolsRowHeightTest checks these against SeedBar.CARD_HEIGHT
  static final float TOOL_BUTTON_HEIGHT = 62f;
  static final float TOOL_BUTTON_PAD = 3f;

  private static final float TOP_BLOCK_LIMIT = 190f;

  private final Stage stage;
  private final HudArt hudArt = new HudArt();

  private Skin skin;
  private Label sunCount;
  /** Kept so the debug "+" beside each counter can act on the running match. */
  private GameManager match;
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
    this.stage = new Stage(new ExtendViewport(GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT));
  }

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
    this.match = match;

    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(4f);
    stage.addActor(root);

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

    Table footer = new Table();
    footer.setFillParent(true);
    footer.bottom().left().pad(6f);
    objective = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    footer.add(objective).left();
    stage.addActor(footer);

    if (DebugPanel.isEnabled()) {
      Table corner = new Table();
      corner.setFillParent(true);
      corner.bottom().right().pad(8f);
      corner.add(new DebugPanel(skin, this::toast, match));
      stage.addActor(corner);
    }
  }

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

  public void alert(String message) {
    Toast.showAlert(stage, skin, message);
  }

  private Table counters(Skin skin) {
    Table box = new Table();
    Drawable plate = HudPlates.plate(skin);
    if (plate != null) {
      box.setBackground(plate);
    }
    box.pad(6f, 16f, 6f, 18f);
    box.add(new CurrencyHud(skin)).left().padRight(20f);
    box.add(sunCounter(skin)).left();
    if (DebugPanel.isEnabled()) {
      box.add(DebugPanel.counterCheat(skin, match, this::toast, false))
          .left().padLeft(6f).width(40f).height(34f);
    }
    return box;
  }

  private Table sunCounter(Skin skin) {
    Table box = new Table();
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

  private Table toolsRow(Skin skin, Runnable onShovel, Runnable onPlantFood, Runnable onPause) {
    Table tools = new Table();
    tools.defaults().pad(TOOL_BUTTON_PAD).width(TOOL_BUTTON_WIDTH).height(TOOL_BUTTON_HEIGHT);
    shovelButton = iconTool(skin, HudPlates.SHOVEL, HudPlates.SHOVEL_ARMED, "Shovel", onShovel);
    plantFoodButton = iconTool(skin, HudPlates.PLANT_FOOD, HudPlates.PLANT_FOOD_ARMED,
        "Food 0", onPlantFood);
    tools.add(shovelButton);
    plantFoodCount = new Label("0", skin, UiSkinProvider.LABEL_MEDIUM_OUTLINE);
    Table countCorner = new Table();
    countCorner.bottom().right().pad(0f, 0f, 6f, 8f);
    countCorner.add(plantFoodCount);
    tools.add(new Stack(plantFoodButton, countCorner));
    if (DebugPanel.isEnabled()) {
      tools.add(DebugPanel.counterCheat(skin, match, this::toast, true))
          .width(40f).height(34f);
    }
    tools.add(iconTool(skin, HudPlates.PAUSE, HudPlates.PAUSE_DOWN, "Pause", onPause));
    return tools;
  }

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

  public void updateTools(boolean shovelArmed, boolean plantFoodArmed, int foodCount) {
    if (shovelButton != null) {
      shovelButton.setChecked(shovelArmed);
      shovelButton.setColor(shovelArmed ? TOOL_ARMED : TOOL_IDLE);
    }
    if (plantFoodButton != null) {
      plantFoodButton.setChecked(plantFoodArmed);
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

  public void toast(String message) {
    Toast.show(stage, skin, message);
  }

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
