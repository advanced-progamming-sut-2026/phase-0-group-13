package view.gdx.core;


public final class GdxConfig {

  public static final String WINDOW_TITLE = "Plants vs. Zombies - Group 13";
  public static final int WINDOW_WIDTH = 1280;
  public static final int WINDOW_HEIGHT = 720;
  public static final int FOREGROUND_FPS = 60;
  public static final boolean USE_VSYNC = true;

  public static final float WORLD_WIDTH = 1280f;

  public static final float WORLD_HEIGHT = 720f;

  public static final int LAWN_ROWS = 5;

  public static final int LAWN_COLUMNS = 9;

  public static final int TICKS_PER_SECOND = 10;

  public static final float SECONDS_PER_TICK = 1f / TICKS_PER_SECOND;

  public static final boolean DEBUG_MODE = Boolean.getBoolean("pvz.debug");

  private static final float MAX_TEXT_SUPERSAMPLE = 2.5f;

  public static float textSupersample() {
    if (com.badlogic.gdx.Gdx.graphics == null) {
      return 1f;
    }
    com.badlogic.gdx.Graphics.DisplayMode mode = com.badlogic.gdx.Gdx.graphics.getDisplayMode();
    if (mode == null || mode.height <= 0) {
      return 1f;
    }
    float scale = mode.height / WORLD_HEIGHT;
    return Math.max(1f, Math.min(MAX_TEXT_SUPERSAMPLE, scale));
  }

  private GdxConfig() {
  }
}
