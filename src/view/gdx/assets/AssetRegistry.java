package view.gdx.assets;


public interface AssetRegistry {

  void queueAll();

  /**
   * Advances loading by one frame's worth of work.
   *
   * @return true once the queue is drained
   */
  boolean update();

  float progress();

  /**
   * Whether a single asset has finished loading.
   *
   * @param assetPath path relative to the classpath root, built from {@link AssetPaths}
   */
  boolean isLoaded(String assetPath);

  void unloadAll();
}
