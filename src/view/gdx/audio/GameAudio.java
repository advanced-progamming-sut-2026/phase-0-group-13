package view.gdx.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import view.gdx.core.GameSettings;

public final class GameAudio implements Disposable {

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

  private boolean silent() {
    return unavailable || Gdx.audio == null || Gdx.files == null;
  }

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
      }
    }
    sounds.clear();
    lastPlayedNanos.clear();
  }
}
