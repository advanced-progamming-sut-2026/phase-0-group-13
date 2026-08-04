package view.gdx.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.ScreenUtils;
import view.gdx.assets.GameAssets;


public class PvzGdxGame extends Game {

  private GameAssets assets;

  public GameAssets getAssets() {
    return assets;
  }

  @Override
  public void create() {
    assets = new GameAssets();
  }

  @Override
  public void render() {
    ScreenUtils.clear(0.04f, 0.14f, 0.07f, 1f);
    super.render();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (assets != null) {
      assets.dispose();
    }
  }
}
