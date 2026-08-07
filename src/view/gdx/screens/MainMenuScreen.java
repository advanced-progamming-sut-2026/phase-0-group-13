package view.gdx.screens;

import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudStage;


/**
 * Placeholder for the graphical main menu.
 *
 * <p>Here so the screens join up: loading hands over to this, and a match would get started from
 * here. No menu has actually been ported yet. The terminal ones in view.MainMenuSubMenus are
 * untouched and still the only working ones.
 */
public final class MainMenuScreen extends BaseScreen {

  private HudStage ui;

  public MainMenuScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  public void show() {
    ui = new HudStage();
    ui.build(game.getUiSkin());
    // TODO send input to ui.getStage() once there's something to click
  }

  @Override
  public void render(float delta) {
    ui.act(delta);
    ui.draw();
    // TODO background and buttons, Play calls game.switchScreen(new GameplayScreen(...))
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    ui.resize(width, height);
  }

  @Override
  public void dispose() {
    if (ui != null) {
      ui.dispose();
    }
  }
}
