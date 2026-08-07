package view.gdx.core;

import com.badlogic.gdx.Game;
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
  }

  @Override
  public void create() {
    assets = new GameAssets();
    context = new RenderContext();
    uiSkin = new UiSkinProvider();
    setScreen(new LoadingScreen(this));
  }

  @Override
  public void render() {
    ScreenUtils.clear(0.04f, 0.14f, 0.07f, 1f);
    super.render();
  }

  @Override
  public void dispose() {
    Screen current = getScreen();
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
