package view.gdx.screens;

import com.badlogic.gdx.Screen;
import view.gdx.assets.GameAssets;
import view.gdx.core.PvzGdxGame;

public abstract class BaseScreen implements Screen {

  protected final PvzGdxGame game;

  protected BaseScreen(PvzGdxGame game) {
    this.game = game;
  }

  protected GameAssets assets() {
    return game.getAssets();
  }

  @Override
  public void show() {}

  @Override
  public void resize(int width, int height) {}

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void hide() {}

  @Override
  public void dispose() {}
}
