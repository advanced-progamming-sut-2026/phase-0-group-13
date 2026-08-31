package view.gdx.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ScreenUtils;
import view.gdx.assets.GameAssets;
import view.gdx.render.RenderContext;
import view.gdx.screens.LoadingScreen;
import view.gdx.ui.UiSkinProvider;


public class PvzGdxGame extends Game {

  private GameAssets assets;
  private RenderContext context;
  private UiSkinProvider uiSkin;
  private InviteWatcher invites;

  public GameAssets getAssets() {
    return assets;
  }

  /** Shared batch, shapes, camera and viewport. Screens borrow these, they don't own them. */
  public RenderContext getContext() {
    return context;
  }

  /** Shared skin source. See UiSkinProvider for why it might give back nothing yet. */
  public UiSkinProvider getUiSkin() {
    return uiSkin;
  }

  /**
   * Swaps the screen and disposes the old one.
   *
   * <p>setScreen() only calls hide() on the old screen, so anything holding native resources
   * leaks. Screens should switch through here.
   */
  public void switchScreen(Screen next) {
    Screen previous = getScreen();
    setScreen(next);
    if (previous != null) {
      previous.dispose();
    }
    if (invites != null) {
      invites.present();
    }
  }

  @Override
  public void create() {
    assets = new GameAssets();
    context = new RenderContext();
    uiSkin = new UiSkinProvider();
    invites = new InviteWatcher(this);
    setScreen(new LoadingScreen(this));
  }

  /**
   * What shows through where the world does not reach: the letterbox bars.
   *
   * <p>Every viewport in the game is a FitViewport on one 16:9 virtual size, so on any window that
   * is not 16:9 this is what the player actually sees down the sides or across the top and bottom.
   * It used to be a lawn green, which on a 21:9 monitor reads as a rendering fault -- two wide
   * green stripes beside the picture. Near-black reads as a frame instead, which is what it is.
   */
  private static final float BORDER_R = 0.05f;
  private static final float BORDER_G = 0.05f;
  private static final float BORDER_B = 0.06f;

  @Override
  public void render() {
    pollFullscreenToggle();
    ScreenUtils.clear(BORDER_R, BORDER_G, BORDER_B, 1f);
    super.render();
  }

  /**
   * F11 (or Alt+Enter) toggles fullscreen.
   *
   * <p>Polled here rather than through a listener because every screen installs its own input
   * processor, so a Scene2D listener would only work on whichever screen registered it. The
   * viewports do the rest: world and UI are both letterboxed to the same virtual size, so the
   * whole game scales to the new resolution instead of shrinking into a corner of it.
   */
  private void pollFullscreenToggle() {
    boolean altEnter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
        && (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT));
    if (!Gdx.input.isKeyJustPressed(Input.Keys.F11) && !altEnter) {
      return;
    }
    if (Gdx.graphics.isFullscreen()) {
      Gdx.graphics.setWindowedMode(GdxConfig.WINDOW_WIDTH, GdxConfig.WINDOW_HEIGHT);
    } else {
      Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }
  }

  @Override
  public void dispose() {
    Screen current = getScreen();
    view.gdx.audio.GameAudio.getInstance().dispose();
    super.dispose();
    if (current != null) {
      current.dispose();
    }
    if (uiSkin != null) {
      uiSkin.dispose();
    }
    if (context != null) {
      context.dispose();
    }
    if (assets != null) {
      assets.dispose();
    }
  }
}
