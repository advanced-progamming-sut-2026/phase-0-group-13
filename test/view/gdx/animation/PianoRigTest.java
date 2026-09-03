package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The Pianist is two rigs, not one.
 *
 * <p>Upstream ships ZOMBIE_PIANO (the zombie) and PIANO (the instrument) as separate animations
 * sharing one atlas page. Only the zombie was wired up, and the atlas listed only the zombie's 28
 * regions out of the 54 packed into that page -- so the Pianist walked the lawn playing nothing.
 */
class PianoRigTest {

  private static final Path ATLAS =
      Path.of("assets", "textures", "zombies", "zombiepianodefault.atlas");
  private static final Path MANIFEST =
      Path.of("assets", "animations", "zombies", "zombiepiano.json");
  private static final Path PIANO_PAM =
      Path.of("resources", "raw", "pvz2", "IMAGES", "768", "FULL", "ZOMBIE", "PIANO", "PIANO.PAM");

  private static Set<String> regionsOf(Path atlas) throws IOException {
    Set<String> names = new TreeSet<>();
    for (String line : Files.readAllLines(atlas)) {
      if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))
          && !line.contains(":") && !line.endsWith(".png")) {
        names.add(line.trim());
      }
    }
    return names;
  }

  /** The image names a PAM references, read out of its printable strings. */
  private static Set<String> rigImages(Path pam) throws IOException {
    Set<String> leaves = new HashSet<>();
    String raw = new String(Files.readAllBytes(pam), StandardCharsets.ISO_8859_1);
    Matcher matcher = Pattern.compile("([A-Za-z0-9_]+)\\|IMAGE_").matcher(raw);
    while (matcher.find()) {
      leaves.add(matcher.group(1));
    }
    return leaves;
  }

  @Test
  void thePianoRigHasAManifestPointingAtFilesThatExist() throws IOException {
    assertTrue(Files.exists(MANIFEST), "the piano has no animation manifest");
    JsonObject root = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
    assertTrue(Files.exists(Path.of("assets", root.get("atlas").getAsString())),
        "the piano manifest points at an atlas that is not there");
    String pam = root.getAsJsonArray("animations").get(0).getAsJsonObject()
        .get("pam").getAsString();
    assertTrue(Files.exists(Path.of("resources", "raw", "pvz2", "IMAGES").resolve(pam)),
        "the piano manifest points at a rig that is not there");
  }

  @Test
  void everyPieceOfThePianoResolvesInTheAtlas() throws IOException {
    Set<String> regions = regionsOf(ATLAS);
    Set<String> missing = new TreeSet<>(rigImages(PIANO_PAM));
    missing.removeAll(regions);
    assertTrue(missing.isEmpty(),
        "the piano would draw with holes in it; unresolved regions: " + missing);
  }

  @Test
  void thePianoAndThePlayerShareOneAtlasPage() throws IOException {
    JsonObject piano = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
    JsonObject zombie = JsonParser.parseString(Files.readString(
        Path.of("assets", "animations", "zombies", "zombiepianodefault.json"))).getAsJsonObject();
    // They are drawn at one position and one scale, which only holds while the two rigs are
    // authored against the same page and the same canvas.
    assertTrue(piano.get("atlas").getAsString().equals(zombie.get("atlas").getAsString()),
        "the piano and its player must come off the same atlas to line up");
  }

  @Test
  void thePianoIsNotMistakenForAZombie() throws IOException {
    JsonObject root = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
    assertFalse(root.get("entity").getAsString().equalsIgnoreCase("ZombiePianoDefault"),
        "the instrument must not shadow the zombie's own rig name");
  }
}
