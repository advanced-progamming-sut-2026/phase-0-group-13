package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;


/**
 * Coin and gem icons for lawn drops.
 *
 * <p>Same lazy, null-returning shape as {@link HudArt}, but pointing at the skin atlas: the two
 * currency icons live there ({@link UiSkinProvider#COIN_ICON} / {@link UiSkinProvider#GEM_ICON})
 * rather than in hud.atlas. EntityRenderer.lootIcon() already skips a pickup whose art is null,
 * so a missing atlas costs the drop its icon and nothing else.
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

  private TextureRegion find(String region) {
    TextureAtlas loaded = atlas();
    return loaded == null ? null : loaded.findRegion(region);
  }

  private TextureAtlas atlas() {
    if (atlas == null && !loadFailed) {
      if (!Gdx.files.classpath(ATLAS_PATH).exists()) {
        loadFailed = true;
        return null;
      }
      atlas = new TextureAtlas(Gdx.files.classpath(ATLAS_PATH));
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
