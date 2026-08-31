package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

  protected RenderContext context() {
    return game.getContext();
  }

  public Stage uiStage() {
    return null;
  }

  @Override
  public void show() {}

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
