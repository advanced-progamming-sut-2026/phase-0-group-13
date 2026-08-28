package view.gdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The four Zombotany zombies are the only entities in the game with no art of their own: the
 * extractor lists all four under "unresolved" in assets/metadata/asset-map.json, so neither
 * zombiepackets.atlas nor a per-zombie rig atlas has anything for them, and they used to fall all
 * the way through to EntityRenderer's outline drawing. ZombieArt.zombotanyPlant maps them onto the
 * seed packet of the plant they are named after.
 *
 * <p>Runs without a LibGDX Application: the mapping is a pure string function, and the atlas is
 * read as a text file off the classpath (assets/ is the main resources srcDir) rather than through
 * TextureAtlas, which would need a GL context. That is enough to catch the failure that matters --
 * a mapping pointing at a region name the packet page does not actually have.
 */
class ZombotanyArtTest {

  private static final String PACKETS = "textures/plants/seedpackets.atlas";

  @Test
  void everyZombotanyAliasMapsToAPlant() {
    assertEquals("peashooter", ZombieArt.zombotanyPlant("ZombieZombotanyPeashooterDefault"));
    assertEquals("wallnut", ZombieArt.zombotanyPlant("ZombieZombotanyWallnutDefault"));
    assertEquals("jalapeno", ZombieArt.zombotanyPlant("ZombieZombotanyJalapenoDefault"));
    assertEquals("squash", ZombieArt.zombotanyPlant("ZombieZombotanySquashDefault"));
  }

  @Test
  void ordinaryZombiesAreLeftAlone() {
    assertNull(ZombieArt.zombotanyPlant("ZombieMummyDefault"));
    assertNull(ZombieArt.zombotanyPlant("basic"));
    assertNull(ZombieArt.zombotanyPlant(null));
    // a plant name on its own is not a Zombotany zombie
    assertNull(ZombieArt.zombotanyPlant("Peashooter"));
  }

  @Test
  void thePlantsTheyMapToAllHaveASeedPacket() throws IOException {
    Set<String> regions = regionNames();
    assertTrue(regions.size() > 20, "seedpackets.atlas did not parse: " + regions.size());
    for (String alias : new String[] {"ZombieZombotanyPeashooterDefault",
        "ZombieZombotanyWallnutDefault", "ZombieZombotanyJalapenoDefault",
        "ZombieZombotanySquashDefault"}) {
      String plant = ZombieArt.zombotanyPlant(alias);
      assertNotNull(plant, alias);
      assertTrue(regions.contains(plant),
          alias + " maps to '" + plant + "', which is not a region in " + PACKETS);
    }
  }

  /**
   * Region names in a libGDX atlas are the unindented lines after the header block; every other
   * line is either a page name (ends in .png) or an indented "key: value" property.
   */
  private static Set<String> regionNames() throws IOException {
    Set<String> names = new HashSet<>();
    InputStream stream = ZombotanyArtTest.class.getClassLoader().getResourceAsStream(PACKETS);
    assertNotNull(stream, PACKETS + " is not on the test classpath");
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank() || line.startsWith(" ") || line.contains(":") || line.endsWith(".png")) {
          continue;
        }
        names.add(line.trim());
      }
    }
    return names;
  }
}
