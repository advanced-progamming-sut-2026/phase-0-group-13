package view.gdx.core;


/**
 * The settings the player can change while the graphical build is running.
 *
 * <p>All of them are view-only: speed scales the frame delta handed to {@link FixedStepClock}, so
 * the model still ticks at {@link GdxConfig#TICKS_PER_SECOND}, it just gets asked for more ticks
 * per frame. Debug starts from the {@code -Dpvz.debug} launcher flag and can then be flipped at
 * runtime, which is what the Settings screen does.
 *
 * <p>The two volumes are 0..1 and are read by view.gdx.audio.GameAudio every time it plays
 * something, so changing one here takes effect without anything needing to be notified. Mute is
 * kept separately from a zero volume so that turning sound off and back on restores the levels the
 * player had set rather than dropping them to nothing.
 */
public final class GameSettings {

  public static final int MIN_SPEED = 1;
  public static final int MAX_SPEED = 3;

  public static final float MIN_VOLUME = 0f;
  public static final float MAX_VOLUME = 1f;

  private static int gameSpeed = MIN_SPEED;
  private static boolean gridVisible = false;
  private static boolean debugMode = GdxConfig.DEBUG_MODE;
  private static float musicVolume = 0.45f;
  private static float sfxVolume = 0.7f;
  private static boolean muted = false;

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

  /** Zero while muted, so a caller never has to check both. */
  public static float getMusicVolume() {
    return muted ? MIN_VOLUME : musicVolume;
  }

  public static void setMusicVolume(float volume) {
    musicVolume = clampVolume(volume);
  }

  /** Zero while muted, so a caller never has to check both. */
  public static float getSfxVolume() {
    return muted ? MIN_VOLUME : sfxVolume;
  }

  public static void setSfxVolume(float volume) {
    sfxVolume = clampVolume(volume);
  }

  public static boolean isMuted() {
    return muted;
  }

  public static void setMuted(boolean value) {
    muted = value;
  }

  private static float clampVolume(float volume) {
    if (Float.isNaN(volume)) {
      return MIN_VOLUME;
    }
    return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
  }
}
