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

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import model.core.GameManager;
import model.game.plant.PlantParts.PlantTemplate;
/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, plant food, wave banner, pause button.
 *
 * <p>Has its own ScreenViewport rather than sharing the world one. The world is letterboxed to a
 * fixed size so the lawn keeps its shape, but the HUD wants one unit per real pixel so the text
 * stays sharp at any window size.
 *
 * <p>What is filled in so far is the part the menus share: the coin and diamond readout, the debug
 * cheats and a way back out. It uses the same CurrencyHud and DebugPanel the menus do, so the
 * balances shown during a match are the same ones shown outside it. The match-specific widgets are
 * still to come.
 *
 * <p>build() does nothing while there's no skin, see UiSkinProvider.
 */
public final class HudStage implements Disposable {

private final Stage stage;

private final HudArt hudArt = new HudArt();

private Label sunCount;
private Label status;

private Table seedBar;
private SeedBar seeds;

private Table toolsRow;
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
    Skin skin = skinProvider.get();

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

    toolsRow = new Table();
    root.add(toolsRow).left().padTop(2f).row();

    if (DebugPanel.isEnabled()) {
      root.add(new DebugPanel(skin, message -> Toast.show(stage, skin, message)))
          .colspan(onExit != null ? 2 : 1)
          .right()
          .padTop(12f);
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


  /** Seed packets for this match. Clicking one arms it for the next click on the lawn. */
  public void buildSeedBar(Skin skin, java.util.List<model.game.plant.PlantParts.PlantTemplate> templates,
      java.util.function.Consumer<String> onPick) {
    if (seedBar == null || skin == null) {
      return;
    }
    seedBar.clear();
    seeds = new SeedBar(skin, null, templates, plant -> {
      selected = plant;
      onPick.accept(plant);
    });
    seedBar.add(seeds);
  }

  public void updateSeeds(model.core.GameManager match, String selected,
      java.util.function.ToIntFunction<String> levelOf) {
    if (seeds != null) {
      seeds.update(match, selected, levelOf);
    }
  }

  /** Shovel (pluck) and plant food toggle buttons, next to the seed bar. */
  public void buildTools(Skin skin, Runnable onShovel, Runnable onPlantFood) {
    if (toolsRow == null || skin == null) {
      return;
    }
    toolsRow.clear();
    shovelButton = new TextButton("Shovel", skin, UiSkinProvider.BUTTON_BROWN);
    shovelButton.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        onShovel.run();
      }
    });
    plantFoodButton = new TextButton("Plant Food", skin, UiSkinProvider.BUTTON_BROWN);
    plantFoodButton.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        onPlantFood.run();
      }
    });
    toolsRow.add(shovelButton).width(110f).height(48f).padRight(6f);
    toolsRow.add(plantFoodButton).width(110f).height(48f);
  }

  /** Highlights whichever tool is currently armed, so the player can tell what a click will do. */
  public void updateTools(boolean shovelArmed, boolean plantFoodArmed) {
    if (shovelButton != null) {
      shovelButton.setColor(shovelArmed ? Color.YELLOW : Color.WHITE);
    }
    if (plantFoodButton != null) {
      plantFoodButton.setColor(plantFoodArmed ? Color.YELLOW : Color.WHITE);
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
