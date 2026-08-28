package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import view.gdx.assets.GameAssets;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.RenderContext;

public abstract class BaseScreen implements Screen {

  protected final PvzGdxGame game;

  protected BaseScreen(PvzGdxGame game) {
    this.game = game;
  }

  /**
   * Runs blocking work (a network round trip, usually) off the render thread, then hands the
   * result back on it. Every UserManager call that reaches {@code ClientSession} blocks on a
   * socket, and calling that straight from a Scene2D {@code ClickListener} freezes the whole
   * window for as long as the request takes - the leaderboard and multiplayer screens already
   * avoided this by hand-rolling a worker thread plus {@code Gdx.app.postRunnable}; this is that
   * same pattern, shared.
   *
   * <p>If the player has navigated to another screen by the time the work finishes, the result is
   * dropped instead of touching a stage nobody can see any more.
   */
  protected <T> void runAsync(
      Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
    Thread worker = new Thread(() -> {
      T result = null;
      Exception failure = null;
      try {
        result = work.call();
      } catch (Exception e) {
        failure = e;
      }
      T finalResult = result;
      Exception finalFailure = failure;
      Gdx.app.postRunnable(() -> {
        if (game.getScreen() != BaseScreen.this) {
          return;
        }
        if (finalFailure != null) {
          onError.accept(finalFailure);
        } else {
          onSuccess.accept(finalResult);
        }
      });
    }, getClass().getSimpleName() + "-async");
    worker.setDaemon(true);
    worker.start();
  }

  protected GameAssets assets() {
    return game.getAssets();
  }

  /** Shared batch, shapes and camera. The game owns them, not the screen. */
  protected RenderContext context() {
    return game.getContext();
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
