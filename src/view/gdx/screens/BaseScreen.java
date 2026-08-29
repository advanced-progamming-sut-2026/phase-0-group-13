package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import view.gdx.assets.GameAssets;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.RenderContext;

public abstract class BaseScreen implements Screen {

  protected final PvzGdxGame game;

  protected BaseScreen(PvzGdxGame game) {
    this.game = game;
  }

  protected GameAssets assets() {
    return game.getAssets();
  }

  /** Shared batch, shapes and camera. The game owns them, not the screen. */
  protected RenderContext context() {
    return game.getContext();
  }

  /**
   * The Scene2D stage a session-wide popup (a match invite) can be put on, or null while this
   * screen has none.
   */
  public Stage uiStage() {
    return null;
  }

  @Override
  public void show() {}

  /**
   * Keeps the world viewport matching the window. Screens with their own viewport (a Scene2D
   * stage, say) should override this and call super.resize first.
   */
  @Override
  public void resize(int width, int height) {
    context().resize(width, height);
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void hide() {}

  @Override
  public void dispose() {}
}
