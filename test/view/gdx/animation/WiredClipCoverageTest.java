package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The rigs already carried these clips; the renderer simply never asked for them, so the artwork
 * sat unused and a plant-food dose, a death and a Gargantuar's smash all looked like nothing
 * happening. This checks the clip names EntityRenderer now asks for are names the rigs answer to
 * -- a typo there fails silently, falling back to idle with no error anywhere.
 *
 * <p>Reads the manifests off disk rather than through AnimationLibrary, which needs a GL context
 * for its atlas, and mirrors {@link EntityAnimation#pickClip}'s rule exactly: an exact name wins,
 * then the shortest that starts with it, then the shortest that merely contains it.
 */
class WiredClipCoverageTest {

  private static final Path PLANTS = Path.of("assets", "animations", "plants");
  private static final Path ZOMBIES = Path.of("assets", "animations", "zombies");

  private static final Path ROSTER = Path.of("src", "data", "database", "plants.json");

  /** Kept in step with EntityRenderer.PLANT_FOOD_CLIPS, which is what the lawn actually asks. */
  private static final String[] PLANT_FOOD_CLIPS =
      {"plantfood", "plantfood_on", "plantfood_loop", "pf"};

  /**
   * Plants with no plant-food clip to show, read off the roster rather than listed here: a plant
   * whose "Plant Food Effect" is "None (single-use plant)" has already gone off by the time a dose
   * could land on it, so the gap is in the game's design and not in the wiring.
   *
   * <p>Read rather than hardcoded because a hardcoded copy goes stale silently the moment a rig is
   * added -- which is exactly what the nine mints did.
   */
  private static Set<String> rigsWithNoPlantFood() throws IOException {
    Set<String> out = new LinkedHashSet<>();
    for (var element : JsonParser.parseString(Files.readString(ROSTER)).getAsJsonArray()) {
      JsonObject plant = element.getAsJsonObject();
      String effect = plant.has("Plant Food Effect")
          ? plant.get("Plant Food Effect").getAsString() : "";
      if (effect.toLowerCase().startsWith("none")) {
        out.add(normalise(plant.get("Name").getAsString()));
      }
    }
    return out;
  }

  /** The rule the extractor names manifests by: lowercase, letters and digits only. */
  private static String normalise(String name) {
    return name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  /**
   * Tall-nut takes a dose -- the roster gives it permanent armour for one -- but its rig carries
   * only idle, damage and damage2, so there is no clip to play and nothing to wire. Listed here
   * on its own rather than folded in with the single-use plants, which are exempt for a different
   * reason entirely; drop this line if the art is ever authored.
   */
  private static final Set<String> NO_PLANT_FOOD_ART = Set.of("tallnut");

  @Test
  void everyPlantThatCanTakePlantFoodHasAClipForIt() throws IOException {
    Set<String> exempt = new LinkedHashSet<>(rigsWithNoPlantFood());
    exempt.addAll(NO_PLANT_FOOD_ART);
    List<String> missing = new ArrayList<>();
    for (Path rig : manifests(PLANTS)) {
      String name = rigName(rig);
      if (exempt.contains(name)) {
        continue;
      }
      if (pick(clipsOf(rig), PLANT_FOOD_CLIPS) == null) {
        missing.add(name);
      }
    }
    assertTrue(missing.isEmpty(),
        "plant rigs with no plantfood clip that were expected to have one: " + missing);
  }

  /**
   * The mints are the only rigs in the library built as intro/loop/outro with no idle at all, and
   * the doc asks for both the idle and the short entry animation. Both have to resolve, or a mint
   * shows a still: "idle" finds nothing on these rigs, which is why the renderer also asks for
   * "loop".
   */
  @Test
  void everyMintHasBothTheClipsTheRendererAsksItFor() throws IOException {
    List<String> rigs = new ArrayList<>();
    for (Path rig : manifests(PLANTS)) {
      if (rigName(rig).endsWith("mint")) {
        rigs.add(rigName(rig));
        List<String> clips = clipsOf(rig);
        assertTrue(clips.contains("intro"), rigName(rig) + " has no intro clip");
        assertTrue(pick(clips, "idle", "loop") != null,
            rigName(rig) + " resolves to no standing clip at all");
      }
    }
    assertEquals(9, rigs.size(), "expected the roster's nine mints to have rigs, found " + rigs);
  }

  @Test
  void everyZombieHasADeathClip() throws IOException {
    List<String> missing = new ArrayList<>();
    for (Path rig : manifests(ZOMBIES)) {
      if (pick(clipsOf(rig), "die", "die2") == null) {
        missing.add(rigName(rig));
      }
    }
    assertTrue(missing.isEmpty(), "zombie rigs with no die clip: " + missing);
  }

  /**
   * The smash is the Gargantuar's and nobody else's, which is what makes it safe for the renderer
   * to key it off the behaviour rather than off the clip being present.
   */
  @Test
  void onlyTheGargantuarsCanSmash() throws IOException {
    Set<String> smashers = new LinkedHashSet<>();
    for (Path rig : manifests(ZOMBIES)) {
      if (pick(clipsOf(rig), "smash_left") != null) {
        smashers.add(rigName(rig));
      }
    }
    assertEquals(Set.of("zombietutorialgargantuar", "zombieegyptgargantuar"), smashers);
  }

  /** The throw has to land on a different clip, or the smash and the imp toss look identical. */
  @Test
  void theGargantuarsThrowResolvesToSomethingOtherThanItsSmash() throws IOException {
    for (String rig : new String[] {"zombietutorialgargantuar", "zombieegyptgargantuar"}) {
      List<String> clips = clipsOf(ZOMBIES.resolve(rig + ".json"));
      String throwing = pick(clips, "fire", "cannon_fire", "particles");
      assertTrue(throwing != null && !throwing.equals("smash_left"),
          rig + " resolved its throw to " + throwing);
    }
  }

  @Test
  void theDefensivePlantsWiredForDamageStagesActuallyHaveThem() throws IOException {
    for (String rig : new String[] {"wallnut", "tallnut", "garlic", "explodeonut", "endurian"}) {
      List<String> clips = clipsOf(PLANTS.resolve(rig + ".json"));
      assertTrue(
          pick(clips, "damage3", "damage2", "idle_damage2", "damage", "idle_damage") != null,
          rig + " has no damage clip but is listed as damage-staged");
    }
  }

  @Test
  void theMinesHaveAnArmedPose() throws IOException {
    for (String rig : new String[] {"potatomine", "primalpotatomine"}) {
      assertTrue(pick(clipsOf(PLANTS.resolve(rig + ".json")), "plant_idle", "plant") != null,
          rig + " has no armed pose");
    }
  }

  @Test
  void theSunProducersHaveTheClipTheyAreWiredTo() throws IOException {
    for (String rig : new String[] {"sunflower", "twinsunflower", "primalsunflower"}) {
      assertTrue(pick(clipsOf(PLANTS.resolve(rig + ".json")), "special") != null,
          rig + " is wired to \"special\" but has no such clip");
    }
  }

  // ---- helpers, mirroring EntityAnimation.pickClip ------------------------------------------

  private static String pick(List<String> clips, String... preferred) {
    for (String want : preferred) {
      if (clips.contains(want)) {
        return want;
      }
    }
    for (String want : preferred) {
      String best = shortest(clips, name -> name.startsWith(want));
      if (best != null) {
        return best;
      }
    }
    for (String want : preferred) {
      String best = shortest(clips, name -> name.contains(want));
      if (best != null) {
        return best;
      }
    }
    return null;
  }

  private static String shortest(List<String> clips, java.util.function.Predicate<String> match) {
    String best = null;
    for (String clip : clips) {
      if (match.test(clip) && (best == null || clip.length() < best.length())) {
        best = clip;
      }
    }
    return best;
  }

  private static List<String> clipsOf(Path manifest) throws IOException {
    JsonObject root = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
    JsonObject clips = root.getAsJsonArray("animations").get(0).getAsJsonObject()
        .getAsJsonObject("clips");
    return clips == null ? List.of() : new ArrayList<>(clips.keySet());
  }

  private static String rigName(Path manifest) {
    return manifest.getFileName().toString().replace(".json", "");
  }

  private static List<Path> manifests(Path dir) throws IOException {
    List<Path> out = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
      stream.forEach(out::add);
    }
    return out;
  }
}
