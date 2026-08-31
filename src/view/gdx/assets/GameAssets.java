package view.gdx.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;


public class GameAssets implements AssetRegistry, Disposable {

  private final AssetManager manager = new AssetManager();

  public AssetManager getManager() {
    return manager;
  }

  public void finishLoading() {
    manager.finishLoading();
  }

  @Override
  public void queueAll() {
  }

  @Override
  public boolean update() {
    return manager.update();
  }

  @Override
  public float progress() {
    return manager.getProgress();
  }

  @Override
  public boolean isLoaded(String assetPath) {
    return manager.isLoaded(assetPath);
  }

  @Override
  public void unloadAll() {
    manager.clear();
  }

  @Override
  public void dispose() {
    manager.dispose();
  }
}
