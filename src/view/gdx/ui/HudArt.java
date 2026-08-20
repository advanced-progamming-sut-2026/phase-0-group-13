package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


/**
 * In-match icon lookups for HUD/entity overlays (sun, pea, plant food, shovel, ...), keyed by a
 * short lowercase name.
 *
 * <p>Same shape as {@link PlantArt}: a single shared atlas, looked up lazily, returning null
 * rather than throwing when the art (or the atlas itself) isn't there yet. {@code textures/ui/}
 * is still empty at the time this class was written (only a .gitkeep), so every lookup returns
 * null for now and callers fall back to drawn shapes -- see EntityRenderer.drawShapes(), which
 * already branches on {@code find(...) == null}. Once a real hud.atlas lands in
 * {@code textures/ui/}, this starts returning real art with no caller changes needed.
 */
public final class HudArt implements Disposable {

  private static final String ATLAS_PATH = "textures/ui/hud.atlas";

  private TextureAtlas atlas;
  private boolean loadFailed;

  /** Icon for this name (e.g. "sun", "pea"), or null if there is none. */
  public TextureRegion find(String iconName) {
    TextureAtlas loaded = atlas();
    if (loaded == null) {
      return null;
    }
    return loaded.findRegion(normalise(iconName));
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
