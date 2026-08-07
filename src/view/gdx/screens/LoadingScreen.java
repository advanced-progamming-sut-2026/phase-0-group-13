package view.gdx.screens;

import view.gdx.core.PvzGdxGame;


/**
 * First screen. Queues the assets, pumps the loader a frame at a time, then hands over.
 *
 * <p>Mostly so nothing else has to check whether the assets are ready.
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
