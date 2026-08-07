package view.gdx.assets;


/**
 * Contract for the future asset loading layer.
 *
 * <p>Deliberately unimplemented. It exists so Phase 2 screens can be written against a stable
 * shape while the real loader (atlas parsing, PAM decoding, caching) is still being designed.
 * The terminal build does not touch this type.
 */
public interface AssetRegistry {

  /** Registers every asset the game needs with the underlying loader. Does not block. */
  void queueAll();

  /**
   * Advances loading by one frame's worth of work.
   *
   * @return true once the queue is drained
   */
  boolean update();

  /** Fraction of the queue that has finished loading, from 0 to 1. */
  float progress();

  /**
   * Whether a single asset has finished loading.
   *
   * @param assetPath path relative to the classpath root, built from {@link AssetPaths}
   */
  boolean isLoaded(String assetPath);

  /** Releases every loaded asset. Safe to call when nothing was ever queued. */
  void unloadAll();
}
