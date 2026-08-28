package view.gdx.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import view.gdx.core.GameSettings;

/**
 * The one place the game makes a noise.
 *
 * <p>Everything goes through the two entry points {@link #play} and {@link #playMusic}, rather than
 * every screen holding its own Sound handles, so muting, the volume sliders and disposal are all
 * decided once. Volumes are read from {@link GameSettings} at the moment of playing, which is what
 * lets the settings screen change them without anything having to be told.
 *
 * <p>It is silent rather than fatal whenever audio is not there: a headless run has no {@code
 * Gdx.audio} at all (the terminal build and the tests never touch a LibGDX Application), a machine
 * can have no output device, and a file can be missing from the build. Each of those is caught
 * once, remembered, and after that the call is a no-op -- so a broken sound never takes a frame
 * down with it and never spams the log.
 *
 * <p>The files themselves are generated, not sampled: see tools/audio-gen/generate_audio.py for
 * why, and re-run it to rebuild them.
 */
public final class GameAudio implements Disposable {

  /** The sound effects, each the basename of a file in {@code sounds/sfx}. */
  public enum Sfx {
    CLICK("click"),
    PLANT("plant"),
    SUN("sun"),
    SHOOT("shoot"),
    CHOMP("chomp"),
    EXPLODE("explode"),
    WIN("win"),
    LOSE("lose");

    private final String file;

    Sfx(String file) {
      this.file = file;
    }
  }

  /** The background tracks, each the basename of a file in {@code sounds/music}. */
  public enum Track {
    MENU("menu"),
    BATTLE("battle");

    private final String file;

    Track(String file) {
      this.file = file;
    }
  }

  private static final String SFX_DIR = "sounds/sfx/";
  private static final String MUSIC_DIR = "sounds/music/";
  private static final String EXTENSION = ".wav";

  /**
   * How close together the same effect may fire, in seconds.
   *
   * <p>A wave can put a dozen zombies on one plant, and a dozen identical bites in the same frame
   * is noise, not a sound effect. Sun collection has the same problem when a plant food clears the
   * lawn. Keeping only the first of a burst is enough to fix it and costs nothing.
   */
  private static final float REPEAT_GUARD_SECONDS = 0.06f;

  private static GameAudio instance;

  private final Map<Sfx, Sound> sounds = new EnumMap<>(Sfx.class);
  private final Map<Sfx, Long> lastPlayedNanos = new EnumMap<>(Sfx.class);
  private Music music;
  private Track playing;
  private boolean unavailable;

  private GameAudio() {
  }

  public static synchronized GameAudio getInstance() {
    if (instance == null) {
      instance = new GameAudio();
    }
    return instance;
  }

  /** Plays an effect at the current effects volume, or does nothing if it cannot. */
  public void play(Sfx effect) {
    if (effect == null || silent()) {
      return;
    }
    float volume = GameSettings.getSfxVolume();
    if (volume <= 0f) {
      return;
    }
    long now = System.nanoTime();
    Long last = lastPlayedNanos.get(effect);
    if (last != null && now - last < REPEAT_GUARD_SECONDS * 1_000_000_000L) {
      return;
    }
    Sound sound = sound(effect);
    if (sound == null) {
      return;
    }
    lastPlayedNanos.put(effect, now);
    try {
      sound.play(volume);
    } catch (RuntimeException e) {
      giveUp("could not play " + effect, e);
    }
  }

  /**
   * Starts a looping track, or changes the volume if that track is already the one playing.
   *
   * <p>Screens call this on show(), so it is asked for the same track over and over as the player
   * moves between menus. Restarting the loop each time would make the music stutter at every
   * navigation, so an unchanged track is left alone.
   */
  public void playMusic(Track track) {
    if (track == null || silent()) {
      return;
    }
    float volume = GameSettings.getMusicVolume();
    if (track == playing && music != null) {
      applyMusicVolume(volume);
      return;
    }
    stopMusic();
    if (volume <= 0f) {
      // Nothing to hear, but remember the choice so raising the slider starts the right track.
      playing = track;
      return;
    }
    FileHandle file = Gdx.files.internal(MUSIC_DIR + track.file + EXTENSION);
    if (!file.exists()) {
      return;
    }
    try {
      music = Gdx.audio.newMusic(file);
      music.setLooping(true);
      music.setVolume(volume);
      music.play();
      playing = track;
    } catch (RuntimeException e) {
      giveUp("could not start the " + track + " music", e);
    }
  }

  /**
   * Pushes the current volume settings out to whatever is playing right now.
   *
   * <p>The effects volume needs nothing -- it is read per play() -- but music is already running,
   * so it has to be told. Turning the music slider up from zero also has to start the track that
   * playMusic() remembered but did not open.
   */
  public void refreshVolumes() {
    if (silent()) {
      return;
    }
    float volume = GameSettings.getMusicVolume();
    if (music == null && playing != null && volume > 0f) {
      Track wanted = playing;
      playing = null;
      playMusic(wanted);
      return;
    }
    applyMusicVolume(volume);
  }

  private void applyMusicVolume(float volume) {
    if (music == null) {
      return;
    }
    try {
      if (volume <= 0f) {
        // Freeing it rather than playing silence: a muted game should not hold an audio device.
        Track wanted = playing;
        stopMusic();
        playing = wanted;
        return;
      }
      music.setVolume(volume);
      if (!music.isPlaying()) {
        music.play();
      }
    } catch (RuntimeException e) {
      giveUp("could not set the music volume", e);
    }
  }

  public void stopMusic() {
    playing = null;
    if (music == null) {
      return;
    }
    try {
      music.stop();
      music.dispose();
    } catch (RuntimeException ignored) {
      // Already gone; there is nothing useful to do about it on the way out.
    }
    music = null;
  }

  private Sound sound(Sfx effect) {
    Sound cached = sounds.get(effect);
    if (cached != null) {
      return cached;
    }
    FileHandle file = Gdx.files.internal(SFX_DIR + effect.file + EXTENSION);
    if (!file.exists()) {
      return null;
    }
    try {
      Sound loaded = Gdx.audio.newSound(file);
      sounds.put(effect, loaded);
      return loaded;
    } catch (RuntimeException e) {
      giveUp("could not load " + effect, e);
      return null;
    }
  }

  /**
   * True when there is no audio to speak to: headless, or something already failed.
   *
   * <p>Deliberately not about mute. Mute is a volume of zero, which the callers already handle,
   * and folding it in here would make refreshVolumes() return before it could act on the mute
   * that was just switched on -- leaving the music playing over a muted game.
   */
  private boolean silent() {
    return unavailable || Gdx.audio == null || Gdx.files == null;
  }

  /** One line, then quiet for the rest of the run. */
  private void giveUp(String what, RuntimeException cause) {
    unavailable = true;
    System.out.println("audio off: " + what + " (" + cause + ")");
  }

  @Override
  public void dispose() {
    stopMusic();
    for (Sound sound : sounds.values()) {
      try {
        sound.dispose();
      } catch (RuntimeException ignored) {
        // same as stopMusic: disposal failures are not worth reporting
      }
    }
    sounds.clear();
    lastPlayedNanos.clear();
  }
}
