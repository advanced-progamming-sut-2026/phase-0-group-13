package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


/**
 * Scene2D layer for the in-match UI: sun counter, seed bar, plant food, wave banner, pause button.
 *
 * <p>Has its own ScreenViewport rather than sharing the world one. The world is letterboxed to a
 * fixed size so the lawn keeps its shape, but the HUD wants one unit per real pixel so the text
 * stays sharp at any window size.
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

  /** Fills in the HUD. Does nothing without a skin, so a screen can just call it. */
  public void build(UiSkinProvider skinProvider) {
    if (!skinProvider.isAvailable()) {
      return;
    }
    // TODO root table, sun counter, seed bar, plant food slots, pause button.
    // The style names are all listed in docs/phase2/pvz-skin-field-guide.html,
    // e.g. ImageButton "ingame_pause" and Label "medium".
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
