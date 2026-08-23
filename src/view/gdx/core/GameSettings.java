package view.gdx.core;


/**
 * The settings the player can change while the graphical build is running.
 *
 * <p>All three are view-only: speed scales the frame delta handed to {@link FixedStepClock}, so the
 * model still ticks at {@link GdxConfig#TICKS_PER_SECOND}, it just gets asked for more ticks per
 * frame. Debug starts from the {@code -Dpvz.debug} launcher flag and can then be flipped at
 * runtime, which is what the Settings screen does.
 */
public final class GameSettings {

  public static final int MIN_SPEED = 1;
  public static final int MAX_SPEED = 3;

  private static int gameSpeed = MIN_SPEED;
  private static boolean gridVisible = true;
  private static boolean debugMode = GdxConfig.DEBUG_MODE;

  private GameSettings() {
  }

  public static int getGameSpeed() {
    return gameSpeed;
  }

  public static void setGameSpeed(int speed) {
    gameSpeed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
  }

  public static boolean isGridVisible() {
    return gridVisible;
  }

  public static void setGridVisible(boolean visible) {
    gridVisible = visible;
  }

  public static boolean isDebugMode() {
    return debugMode;
  }

  public static void setDebugMode(boolean enabled) {
    debugMode = enabled;
  }
}
