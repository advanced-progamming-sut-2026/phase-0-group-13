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

  public RenderContext getContext() {
    return context;
  }

  public UiSkinProvider getUiSkin() {
    return uiSkin;
  }

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

  /** Cleared to this each frame; the viewports cover the window, so it should never be seen. */
  private static final float BORDER_R = 0.05f;
  private static final float BORDER_G = 0.05f;
  private static final float BORDER_B = 0.06f;

  @Override
  public void render() {
    pollFullscreenToggle();
    ScreenUtils.clear(BORDER_R, BORDER_G, BORDER_B, 1f);
    super.render();
  }

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
