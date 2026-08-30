package view.gdx.render;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.game.minigame.arcade.IZombieEngine;
import org.junit.jupiter.api.Test;

/**
 * Every zombie the I, Zombie engine can deploy has to be drawable, or the mandatory two-player
 * screen shows a card labelled "no art". Pole Vaulter, Digger and Ladder have no art of their own
 * in this library and stand in another rig, which is what these pin.
 *
 * <p>No LibGDX Application: the look table is a pure map and the atlases are read as text off the
 * classpath, the same way ZombotanyArtTest does it.
 */
class ArcadeLooksTest {

  private static final String PACKETS = "textures/zombies/zombiepackets.atlas";

  private static List<String> everyEngineZombie() {
    List<String> names = new ArrayList<>();
    for (int level = 1; level <= 3; level++) {
      for (IZombieEngine.ZombieSpec spec : new IZombieEngine(level).availableZombieTypes()) {
        if (!names.contains(spec.name)) {
          names.add(spec.name);
        }
      }
    }
    return names;
  }

  @Test
  void everyDeployableZombieHasALook() {
    for (String name : everyEngineZombie()) {
      assertNotNull(ArcadeRenderer.lookOf(name), name + " has no Look, so it would draw as an outline");
    }
  }

  @Test
  void everyLookNamesAPortraitThePacketPageHas() throws IOException {
    Set<String> regions = regionsOf(PACKETS);
    for (String name : everyEngineZombie()) {
      ArcadeRenderer.Look look = ArcadeRenderer.lookOf(name);
      assertTrue(regions.contains(look.portrait()),
          name + " points at portrait '" + look.portrait() + "' which is not in " + PACKETS);
    }
  }

  @Test
  void theThreeSubstitutesResolve() {
    assertNotNull(ArcadeRenderer.lookOf("pole-vaulter"));
    assertNotNull(ArcadeRenderer.lookOf("digger"));
    assertNotNull(ArcadeRenderer.lookOf("ladder"));
  }

  private static Set<String> regionsOf(String path) throws IOException {
    Set<String> regions = new HashSet<>();
    try (InputStream in = ArcadeLooksTest.class.getClassLoader().getResourceAsStream(path)) {
      assertNotNull(in, path + " is not on the classpath");
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isEmpty() && !line.startsWith(" ") && !line.contains(":")) {
          regions.add(line.trim());
        }
      }
    }
    return regions;
  }
}
