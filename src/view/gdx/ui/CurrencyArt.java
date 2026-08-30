package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * The coin and the gem, for the pickups a zombie drops on the lawn.
 *
 * <p>Same lazy shape as {@link HudArt} and {@link PlantArt}: the atlas is opened on the first
 * lookup, a miss is cached so a broken path is not retried every frame, and every getter returns
 * null rather than throwing, which is what lets EntityRenderer skip a pickup it cannot draw.
 *
 * <p>The one difference is where it reads from. These two icons exist only on the Scene2D skin's
 * page -- {@code hud.atlas} carries the pot but neither the coin nor the gem -- so this opens the
 * skin's atlas directly. That is a second copy of a 2048x2048 page, because the skin the menus use
 * is a {@link com.badlogic.gdx.scenes.scene2d.ui.Skin} owned by PvzGdxGame and a renderer has no
 * route to it. It is opened only once a match actually drops something, and released with the
 * renderer, so it costs nothing outside a match. Worth revisiting if the extractor ever pulls
 * these two icons into {@code textures/ui/hud.atlas}, at which point this becomes a HudArt lookup
 * and the whole class can go.
 */
public final class CurrencyArt implements Disposable {

  /** Where build.gradle's processResources puts resources/skin. See UiSkinProvider. */
  private static final String SKIN_ATLAS = "skin/pvz2_skin.atlas";

  private static final String COIN_REGION = UiSkinProvider.COIN_ICON;
  private static final String GEM_REGION = UiSkinProvider.GEM_ICON;

  private TextureAtlas atlas;
  private boolean loadFailed;

  /** The coin a zombie drops, or null when the skin page is not there. */
  public TextureRegion findCoin() {
    return find(COIN_REGION);
  }

  /** The diamond a zombie drops, or null when the skin page is not there. */
  public TextureRegion findGem() {
    return find(GEM_REGION);
  }

  private TextureRegion find(String regionName) {
    TextureAtlas loaded = atlas();
    return loaded == null ? null : loaded.findRegion(regionName);
  }

  private TextureAtlas atlas() {
    if (atlas == null && !loadFailed) {
      if (!Gdx.files.classpath(SKIN_ATLAS).exists()) {
        loadFailed = true;
        return null;
      }
      atlas = new TextureAtlas(Gdx.files.classpath(SKIN_ATLAS));
    }
    return atlas;
  }

  @Override
  public void dispose() {
    if (atlas != null) {
      atlas.dispose();
      atlas = null;
    }
  }
}
