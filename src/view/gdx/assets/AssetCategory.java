package view.gdx.assets;


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
