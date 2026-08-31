package view.gdx.screens;

import model.core.App;
import view.gdx.core.PvzGdxGame;


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
  }
}
