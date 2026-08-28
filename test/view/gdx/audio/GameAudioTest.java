package view.gdx.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import view.gdx.core.GameSettings;

/**
 * The audio layer has to survive having no audio.
 *
 * <p>There is no LibGDX Application here, so {@code Gdx.audio} and {@code Gdx.files} are both null
 * -- exactly the situation the terminal build, the server and the rest of this suite run in. Every
 * entry point must be a silent no-op in that state rather than an NPE, which is what this checks;
 * without it, adding a sound to a code path shared with the terminal build would take the terminal
 * build down.
 */
class GameAudioTest {

  @AfterEach
  void restoreDefaults() {
    GameSettings.setMuted(false);
    GameSettings.setMusicVolume(0.45f);
    GameSettings.setSfxVolume(0.7f);
  }

  @Test
  void everyEntryPointIsSilentWithoutAnAudioDevice() {
    GameAudio audio = GameAudio.getInstance();
    assertDoesNotThrow(() -> {
      for (GameAudio.Sfx effect : GameAudio.Sfx.values()) {
        audio.play(effect);
      }
      for (GameAudio.Track track : GameAudio.Track.values()) {
        audio.playMusic(track);
      }
      audio.play(null);
      audio.playMusic(null);
      audio.refreshVolumes();
      audio.stopMusic();
      audio.dispose();
    });
  }

  @Test
  void volumesAreClampedToTheSliderRange() {
    GameSettings.setMusicVolume(5f);
    assertEquals(GameSettings.MAX_VOLUME, GameSettings.getMusicVolume());
    GameSettings.setMusicVolume(-3f);
    assertEquals(GameSettings.MIN_VOLUME, GameSettings.getMusicVolume());
    GameSettings.setSfxVolume(Float.NaN);
    assertEquals(GameSettings.MIN_VOLUME, GameSettings.getSfxVolume());
    GameSettings.setSfxVolume(0.5f);
    assertEquals(0.5f, GameSettings.getSfxVolume());
  }

  @Test
  void mutingSilencesBothWithoutLosingTheChosenLevels() {
    GameSettings.setMusicVolume(0.8f);
    GameSettings.setSfxVolume(0.6f);

    GameSettings.setMuted(true);
    assertEquals(0f, GameSettings.getMusicVolume(), "music should be silent while muted");
    assertEquals(0f, GameSettings.getSfxVolume(), "effects should be silent while muted");

    GameSettings.setMuted(false);
    assertEquals(0.8f, GameSettings.getMusicVolume(), "un-muting must restore the level");
    assertEquals(0.6f, GameSettings.getSfxVolume(), "un-muting must restore the level");
  }

  @Test
  void everyDeclaredSoundHasAFileBehindIt() {
    // The enum names the file it plays, so a typo in either is a sound that silently never fires.
    for (GameAudio.Sfx effect : GameAudio.Sfx.values()) {
      assertFileExists("assets/sounds/sfx/" + effect.name().toLowerCase() + ".wav");
    }
    for (GameAudio.Track track : GameAudio.Track.values()) {
      assertFileExists("assets/sounds/music/" + track.name().toLowerCase() + ".wav");
    }
  }

  private static void assertFileExists(String path) {
    File file = new File(path);
    assertTrue(file.isFile(), path + " is missing; run tools/audio-gen/generate_audio.py");
    assertNotNull(file.getName());
    // 44 bytes is a WAV header with no samples after it.
    assertTrue(file.length() > 44, path + " has a header but no audio in it");
  }
}
