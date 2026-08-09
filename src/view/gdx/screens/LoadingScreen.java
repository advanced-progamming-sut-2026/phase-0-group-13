package view.gdx.screens;

import model.core.App;
import view.gdx.core.PvzGdxGame;


/**
 * First screen. Loads the Phase 1 data, queues the assets, pumps the loader a frame at a time,
 * then hands over.
 *
 * <p>Mostly so nothing else has to check whether things are ready.
 *
 * <p>App.initData() is the same call AppView makes before the terminal loop starts: it fills the
 * plant, quest and zombie repositories and restores a stay-logged-in session. Without it the
 * graphical build would come up with no accounts to log into. It is guarded against running twice,
 * so the two entry points don't clash.
 *
 * <p>queueAll() queues nothing right now, so this finishes on the first frame and goes straight to
 * the menu. It still works the same once there are real assets.
 */
public final class LoadingScreen extends BaseScreen {

  private boolean handedOver;

  public LoadingScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  public void show() {
    App.initData();
    assets().queueAll();
  }

  @Override
  public void render(float delta) {
    if (handedOver) {
      return;
    }
    if (assets().update()) {
      handedOver = true;
      game.switchScreen(new MainMenuScreen(game));
      return;
    }
    // TODO loading bar, using assets().progress()
  }
}
