package view.gdx.assets;


/**
 * The asset categories the runtime tree is split into.
 *
 * <p>Each constant names the subfolder it lives in, so a future loader can build a path without
 * hardcoding strings at every call site. Phase 2 infrastructure only: no loading, no caching.
 */
public enum AssetCategory {

  PLANTS("plants"),
  ZOMBIES("zombies"),
  ENVIRONMENT("environment"),
  LAWN("lawn"),
  UI("ui"),
  EFFECTS("effects"),
  SFX("sfx"),
  MUSIC("music");

  private final String folder;

  AssetCategory(String folder) {
    this.folder = folder;
  }

  public String getFolder() {
    return folder;
  }
}
