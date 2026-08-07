package view.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;

public class DesktopLauncher {

  private DesktopLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle(GdxConfig.WINDOW_TITLE);
    config.setWindowedMode(GdxConfig.WINDOW_WIDTH, GdxConfig.WINDOW_HEIGHT);
    config.setForegroundFPS(GdxConfig.FOREGROUND_FPS);
    config.useVsync(GdxConfig.USE_VSYNC);
    new Lwjgl3Application(new PvzGdxGame(), config);
  }
}
