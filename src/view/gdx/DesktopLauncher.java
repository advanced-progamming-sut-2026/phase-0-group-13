package view.gdx;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;

public class DesktopLauncher {

  private DesktopLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle(GdxConfig.WINDOW_TITLE);
    // Borderless at the size of the desktop, rather than setFullscreenMode, which takes the
    // display over and switches it to that mode: on a Retina Mac that drops the panel out of its
    // scaled mode for as long as the game is open, so the menu bar and everything else on the
    // machine changes size around it. A window the size of the desktop covers the screen just the
    // same and leaves the display alone -- and keeps the Retina backbuffer, so the art stays sharp.
    Graphics.DisplayMode desktop = Lwjgl3ApplicationConfiguration.getDisplayMode();
    config.setWindowedMode(desktop.width, desktop.height);
    // Maximised rather than placed at 0,0: the desktop size counts the menu bar, so a window that
    // tall starts underneath it and loses the top of the HUD behind it. Maximising fills the
    // usable area instead, which is the same thing everywhere the menu bar is not.
    config.setMaximized(true);
    config.setForegroundFPS(GdxConfig.FOREGROUND_FPS);
    config.useVsync(GdxConfig.USE_VSYNC);
    new Lwjgl3Application(new PvzGdxGame(), config);
  }
}
