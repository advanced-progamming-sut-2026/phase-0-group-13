package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


/**
 * Plant portraits for the menus, taken from the seed-packet page.
 *
 * <p>The per-plant atlases are loose body parts, so the packets are the only place with one
 * finished picture per plant. Regions are named after the plant with the punctuation stripped
 * (see SEED_PACKETS in tools/asset-extract/extract_assets.py). A few plants have no packet and
 * come back null, which is better than showing the wrong plant.
 */
public final class PlantArt implements Disposable {

  private static final String ATLAS_PATH = "textures/plants/seedpackets.atlas";

  private TextureAtlas atlas;
  private boolean loadFailed;

  /** Portrait for this plant, or null if it has no packet. */
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
