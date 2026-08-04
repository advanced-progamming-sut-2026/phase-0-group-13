package view.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import view.gdx.core.PvzGdxGame;

public class DesktopLauncher {

  private static final String WINDOW_TITLE = "Plants vs. Zombies - Group 13";
  private static final int WINDOW_WIDTH = 1280;
  private static final int WINDOW_HEIGHT = 720;

  private DesktopLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle(WINDOW_TITLE);
    config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
    config.setForegroundFPS(60);
    config.useVsync(true);
    new Lwjgl3Application(new PvzGdxGame(), config);
  }
}
