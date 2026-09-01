package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


public final class PlantArt implements Disposable {

  private static final String ATLAS_PATH = "textures/plants/seedpackets.atlas";
  private static final String RIG_DIR = "textures/plants/";

  private final java.util.Map<String, TextureAtlas> rigs = new java.util.HashMap<>();
  private TextureAtlas atlas;
  private boolean loadFailed;

  public TextureRegion find(String plantName) {
    TextureAtlas loaded = atlas();
    if (loaded == null) {
      return null;
    }
    return loaded.findRegion(normalise(plantName));
  }

  public boolean has(String plantName) {
    return find(plantName) != null;
  }

  /**
   * One region out of a single plant's own rig atlas, by its upstream name, the same way
   * {@link ZombieArt#findPart} reaches into a zombie's. For borrowing a piece of art a rig already
   * ships rather than adding another copy of it.
   *
   * @return the region, or null when there is no such atlas or no such region in it
   */
  public TextureRegion findPart(String rig, String regionName) {
    if (rig == null || regionName == null) {
      return null;
    }
    String key = normalise(rig);
    TextureAtlas found = rigs.get(key);
    if (found == null) {
      String path = RIG_DIR + key + ".atlas";
      if (!Gdx.files.internal(path).exists()) {
        return null;
      }
      found = new TextureAtlas(Gdx.files.internal(path));
      rigs.put(key, found);
    }
    return found.findRegion(regionName);
  }

  private TextureAtlas atlas() {
    if (atlas == null && !loadFailed) {
      if (!Gdx.files.internal(ATLAS_PATH).exists()) {
        loadFailed = true;
        return null;
      }
      atlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
    }
    return atlas;
  }

  private static String normalise(String name) {
    return name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  @Override
  public void dispose() {
    if (atlas != null) {
      atlas.dispose();
      atlas = null;
    }
    for (TextureAtlas rig : rigs.values()) {
      rig.dispose();
    }
    rigs.clear();
  }
}
