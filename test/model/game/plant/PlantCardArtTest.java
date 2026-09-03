package model.game.plant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Asset wiring for the plant roster, checked against the files rather than at runtime.
 *
 * <p>These are the failures that make a plant look broken without anything throwing: a seed card
 * that draws nothing, or an animation manifest pointing at a rig that is not there. Both were
 * found by hand during the audit; this is what stops them coming back.
 */
class PlantCardArtTest {

  private static final Path ASSETS = Path.of("assets");
  private static final Path RIGS = Path.of("resources/raw/pvz2/IMAGES");
  private static final Path PLANTS_JSON = Path.of("src/data/database/plants.json");

  /** Cat-tail ships no art anywhere in the project -- no rig, no atlas, no card. */
  private static final Set<String> NO_ART_IN_PROJECT = Set.of("cattail");

  private static String key(String name) {
    return name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private static List<String> plantNames() throws IOException {
    return JsonParser.parseString(Files.readString(PLANTS_JSON)).getAsJsonArray().asList().stream()
        .map(e -> e.getAsJsonObject().get("Name").getAsString()).toList();
  }

  /** Region names in a libGDX atlas: the unindented lines that are not the page or a property. */
  private static Set<String> regionsOf(Path atlas) throws IOException {
    Set<String> names = new HashSet<>();
    for (String line : Files.readAllLines(atlas)) {
      if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))
          && !line.contains(":") && !line.endsWith(".png")) {
        names.add(line.trim());
      }
    }
    return names;
  }

  @Test
  void everyPlantHasSomethingToDrawOnItsSeedCard() throws IOException {
    Set<String> packets = regionsOf(ASSETS.resolve("textures/plants/seedpackets.atlas"));
    List<String> blank = new ArrayList<>();
    for (String name : plantNames()) {
      String key = key(name);
      if (NO_ART_IN_PROJECT.contains(key) || packets.contains(key)) {
        continue;
      }
      // No packet art, so PlantArt falls back to a portrait out of the plant's own rig atlas.
      Path rig = ASSETS.resolve("textures/plants/" + key + ".atlas");
      assertTrue(Files.exists(rig),
          name + " has no seed-packet card and no rig atlas to take a portrait from");
      if (regionsOf(rig).isEmpty()) {
        blank.add(name);
      }
    }
    assertTrue(blank.isEmpty(), "these plants would draw a blank seed card: " + blank);
  }

  @Test
  void thePortraitFallbacksPointAtRegionsThatExist() throws IOException {
    // Mirrors PlantArt.RIG_PORTRAITS. A typo here draws nothing and throws nothing.
    String[][] portraits = {
        {"rotobaga", "rotorutabaga_123x123"},
        {"piercemint", "spearmint_111x116"},
        {"cattailmint", "ailmint_141x163"},
    };
    for (String[] portrait : portraits) {
      Path atlas = ASSETS.resolve("textures/plants/" + portrait[0] + ".atlas");
      assertTrue(Files.exists(atlas), "no atlas for " + portrait[0]);
      assertTrue(regionsOf(atlas).contains(portrait[1]),
          portrait[0] + " has no region " + portrait[1] + "; its seed card would be blank");
    }
  }

  @Test
  void everyAnimationManifestPointsAtAnAtlasAndARigThatExist() throws IOException {
    List<String> broken = new ArrayList<>();
    try (var manifests = Files.list(ASSETS.resolve("animations/plants"))) {
      for (Path manifest : manifests.filter(p -> p.toString().endsWith(".json")).toList()) {
        JsonObject root =
            JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        Path atlas = ASSETS.resolve(root.get("atlas").getAsString());
        if (!Files.exists(atlas)) {
          broken.add(manifest.getFileName() + " -> missing atlas " + atlas);
        }
        String pam = root.getAsJsonArray("animations").get(0).getAsJsonObject()
            .get("pam").getAsString();
        if (!Files.exists(RIGS.resolve(pam))) {
          broken.add(manifest.getFileName() + " -> missing rig " + pam);
        }
      }
    }
    assertTrue(broken.isEmpty(), "animation manifests pointing at files that are not there: "
        + broken);
  }

  @Test
  void catTailIsStillTheOnlyPlantWithNoRigAtAll() throws IOException {
    List<String> rigless = new ArrayList<>();
    for (String name : plantNames()) {
      if (!Files.exists(ASSETS.resolve("animations/plants/" + key(name) + ".json"))) {
        rigless.add(name);
      }
    }
    // Held deliberately: if another plant loses its rig this fails, and if Cat-tail ever gains
    // one this fails too and NO_ART_IN_PROJECT should be emptied.
    assertTrue(rigless.equals(List.of("Cat-tail")),
        "expected only Cat-tail to have no animation rig, found " + rigless);
  }
}
