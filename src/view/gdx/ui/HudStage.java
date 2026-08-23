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
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import model.core.GameManager;
import model.game.plant.PlantParts.PlantTemplate;

/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, the shovel and plant food tools, the
 * pause button and the pause menu itself.
 *
 * <p>Has its own ScreenViewport rather than sharing the world one. The world is letterboxed to a
 * fixed size so the lawn keeps its shape, but the HUD wants one unit per real pixel so the text
 * stays sharp at any window size.
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

  private final Stage stage;
  private final HudArt hudArt = new HudArt();

  private Skin skin;
  private Label sunCount;
  private Label status;

  private Table seedBar;
  private SeedBar seeds;

  private TextButton shovelButton;
  private TextButton plantFoodButton;

  private String selected;

  public HudStage() {
    this.stage = new Stage(new ScreenViewport());
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
  public void build(UiSkinProvider skinProvider, Runnable onExit) {
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
    status = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    topRow.add(status).left().padLeft(22f).expandX();
    if (onExit != null) {
      topRow.add(exitButton(skin, onExit)).right().width(120f).height(46f);
    }
    root.add(topRow).growX().row();

    seedBar = new Table();
    root.add(seedBar).left().padTop(2f).row();

    if (DebugPanel.isEnabled()) {
      root.add(new DebugPanel(skin, this::toast)).right().padTop(12f);
      root.row();
    }
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

    Table tools = new Table();
    tools.defaults().pad(3f).width(112f).height(48f);
    shovelButton = toolButton(skin, "Shovel", onShovel);
    plantFoodButton = toolButton(skin, "Food 0", onPlantFood);
    tools.add(shovelButton).row();
    tools.add(plantFoodButton).row();
    tools.add(toolButton(skin, "Pause", onPause)).row();
    seedBar.add(tools).left().padLeft(10f).top();
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
    hudArt.dispose();
    stage.dispose();
  }
}
