package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


/**
 * Coin and gem icon lookup for anything drawn with a raw SpriteBatch (loot dropped on the lawn),
 * where a Scene2D {@link com.badlogic.gdx.scenes.scene2d.ui.Skin} isn't available.
 *
 * <p>Points at the same skin atlas {@link UiSkinProvider} draws {@link CurrencyHud}'s icons from
 * (skin/pvz2_skin.atlas), via the region names it already defines, so a coin on the lawn and the
 * coin in the header are the same art. Same shape as {@link HudArt}: lazy, returns null instead of
 * throwing when the atlas isn't there.
 */
public final class CurrencyArt implements Disposable {

  private static final String ATLAS_PATH = "skin/pvz2_skin.atlas";

  private TextureAtlas atlas;
  private boolean loadFailed;

  public TextureRegion findCoin() {
    return find(UiSkinProvider.COIN_ICON);
  }

  public TextureRegion findGem() {
    return find(UiSkinProvider.GEM_ICON);
  }

  private TextureRegion find(String regionName) {
    TextureAtlas loaded = atlas();
    return loaded == null ? null : loaded.findRegion(regionName);
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

  @Override
  public void dispose() {
    if (atlas != null) {
      atlas.dispose();
      atlas = null;
    }
  }
}
