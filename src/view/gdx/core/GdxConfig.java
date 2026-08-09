package view.gdx.core;


/**
 * Window, world and tick constants for the graphical version.
 *
 * <p>These used to be private fields in DesktopLauncher, but the camera and the clock need them
 * too, so they live here now. The terminal version never reads this class.
 */
public final class GdxConfig {

  public static final String WINDOW_TITLE = "Plants vs. Zombies - Group 13";
  public static final int WINDOW_WIDTH = 1280;
  public static final int WINDOW_HEIGHT = 720;
  public static final int FOREGROUND_FPS = 60;
  public static final boolean USE_VSYNC = true;

  /** Virtual size the camera works in. Same as the window for now, so 1 unit is 1 pixel. */
  public static final float WORLD_WIDTH = 1280f;

  public static final float WORLD_HEIGHT = 720f;

  /**
   * Default lawn size, same as the ROWS/COLS that the launchers in model.core use. LawnGeometry
   * takes the real size from the board when there is one.
   */
  public static final int LAWN_ROWS = 5;

  public static final int LAWN_COLUMNS = 9;

  /**
   * The model counts everything in ticks and divides by 10 to get seconds (GameState and
   * PlantFactory both do it), so we have to step at the same rate or the game runs at the wrong
   * speed.
   */
  public static final int TICKS_PER_SECOND = 10;

  public static final float SECONDS_PER_TICK = 1f / TICKS_PER_SECOND;

  /**
   * Whether the graphical build shows the debug controls (the coin/diamond cheats).
   *
   * <p>Off unless the launcher is started with {@code -Dpvz.debug=true}, which
   * {@code ./gradlew runGdx -Ppvz.debug=true} does for you. The terminal version has no such
   * switch, its cheats are always typeable, so this only gates what the UI draws.
   */
  public static final boolean DEBUG_MODE = Boolean.getBoolean("pvz.debug");

  private GdxConfig() {
  }
}
