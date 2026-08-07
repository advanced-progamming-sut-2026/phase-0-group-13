package view.gdx.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;


/**
 * The AssetRegistry implementation, using libGDX's AssetManager.
 *
 * <p>AssetRegistry was left without one so screens could be written against it. This fills it in.
 * The queue is still empty, but callers go through the interface now instead of grabbing the
 * manager themselves.
 *
 * <p>queueAll() stays empty until the asset pipeline is done, see open items 1 and 2 in
 * docs/phase2/asset-organization.md. An empty queue isn't a special case for update(), it just
 * says it's finished on the first frame.
 */
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
    // TODO queue the atlases, fonts and sounds with AssetPaths and AssetCategory once they exist
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
