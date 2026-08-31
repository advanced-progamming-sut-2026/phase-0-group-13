package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


public final class PlantArt implements Disposable {

  private static final String ATLAS_PATH = "textures/plants/seedpackets.atlas";

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
  }
}
