package view.gdx.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;


public class GameAssets implements Disposable {

  private final AssetManager manager = new AssetManager();

  public AssetManager getManager() {
    return manager;
  }

  public void finishLoading() {
    manager.finishLoading();
  }

  @Override
  public void dispose() {
    manager.dispose();
  }
}
