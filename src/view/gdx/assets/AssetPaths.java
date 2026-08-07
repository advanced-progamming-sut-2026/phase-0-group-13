package view.gdx.assets;


/**
 * Root folders of the runtime asset tree, relative to the classpath root.
 *
 * <p>The {@code assets/} directory is registered as a resources source dir in build.gradle, so
 * everything under it is reachable at runtime via {@code Gdx.files.internal(...)} using these
 * prefixes. Phase 2 infrastructure only: nothing here reads or loads a file.
 *
 * <p>The bulk upstream archive under {@code resources/raw/} is a source library, not a runtime
 * root. It is never addressed through this class. See docs/phase2/asset-organization.md.
 */
public final class AssetPaths {

  public static final String TEXTURES = "textures/";
  public static final String ANIMATIONS = "animations/";
  public static final String SOUNDS = "sounds/";
  public static final String FONTS = "fonts/";
  public static final String METADATA = "metadata/";

  private AssetPaths() {
  }
}
