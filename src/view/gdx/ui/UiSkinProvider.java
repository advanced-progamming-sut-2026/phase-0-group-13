package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;


/**
 * One place to get the Scene2D Skin, so the choice only gets made once.
 *
 * <p>Open item 3 in docs/phase2/asset-organization.md is the reason this class exists. There are
 * two possible sources and they clash:
 *
 * <ul>
 *   <li>the pvz-skin library, where PvzSkin.get() loads
 *       Gdx.files.classpath("skin/pvz2_skin.json") out of its own jar;
 *   <li>the copy already sitting in resources/skin/, which would land at that same classpath path
 *       if we copied it into assets/skin/.
 * </ul>
 *
 * <p>Two files at one path is a silent bug that depends on jar order, so neither one is wired up.
 * The library isn't a dependency and resources/ is kept off the classpath. Whichever we go with,
 * only this class has to change.
 *
 * <p>A skin uploads a texture atlas, so it needs the GL context. Don't call get() from a static
 * field or a constructor that runs before the app starts.
 */
public final class UiSkinProvider implements Disposable {

  private Skin skin;

  /**
   * The shared skin, or null while nothing is wired up. Check isAvailable() first. This way
   * screens still work before we've picked a source.
   */
  public Skin get() {
    // TODO sort out open item 3, then load it once and keep it in skin
    return skin;
  }

  /** Whether get() will give back something usable. */
  public boolean isAvailable() {
    return get() != null;
  }

  @Override
  public void dispose() {
    if (skin != null) {
      skin.dispose();
      skin = null;
    }
  }
}
