package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


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

    // TODO seed bar, plant food slots and the pause button. The style names are all listed in
    // docs/phase2/pvz-skin-field-guide.html, e.g. ImageButton "ingame_pause".
    Table root = new Table();
    root.setFillParent(true);
    root.top().pad(16f);
    stage.addActor(root);

    root.add(new CurrencyHud(skin)).left().expandX();
    if (onExit != null) {
      root.add(exitButton(skin, onExit)).right();
    }
    root.row();

    if (DebugPanel.isEnabled()) {
      root.add(new DebugPanel(skin, message -> Toast.show(stage, skin, message)))
          .colspan(onExit != null ? 2 : 1)
          .right()
          .padTop(12f);
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
    stage.dispose();
  }
}
