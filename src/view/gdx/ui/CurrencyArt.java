package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public final class CurrencyArt implements Disposable {

  private static final String SKIN_ATLAS = "skin/pvz2_skin.atlas";

  private static final String COIN_REGION = UiSkinProvider.COIN_ICON;
  private static final String GEM_REGION = UiSkinProvider.GEM_ICON;

  private TextureAtlas atlas;
  private boolean loadFailed;

  public TextureRegion findCoin() {
    return find(COIN_REGION);
  }

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
