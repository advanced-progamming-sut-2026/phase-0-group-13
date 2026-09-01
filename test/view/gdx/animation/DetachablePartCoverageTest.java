package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import view.gdx.render.ArmourParts;
import view.gdx.render.BodyParts;

/**
 * The heads, arms and armour that come off a zombie are matched by upstream PopCap part names, and
 * a name that no longer matches fails in silence: no piece is thrown, nothing is hidden, and the
 * zombie simply dies in one piece as it always did. That is how this artwork sat unused in the
 * first place, so the names are pinned here.
 *
 * <p>Reads the rigs off disk the way {@link WiredClipCoverageTest} does. The atlas needs a GL
 * context and the parts do not, so only the PAM is opened.
 */
class DetachablePartCoverageTest {

  private static final Path ZOMBIES = Path.of("assets", "animations", "zombies");

  /**
   * The rigs with no flying head, which is how they are authored: a Zomboss is a machine, and a
   * Gargantuar's head comes off in its own die clip rather than as a gib.
   */
  private static final Set<String> NO_HEAD_GIB = new TreeSet<>(List.of(
      "zombiebeachfisherman", "zombiedarkking", "zombieegyptgargantuar", "zombieiceagedodo",
      "zombietutorialgargantuar", "zombiezombossmechcowboy", "zombiezombossmechdark",
      "zombiezombossmechegypt", "zombiezombossmechpirate"));

  /** The same, for arms. The Fisherman and the King keep an arm gib but have no head one. */
  private static final Set<String> NO_ARM_GIB = new TreeSet<>(List.of(
      "zombieegyptgargantuar", "zombieiceagedodo", "zombietutorialgargantuar",
      "zombiezombossmechcowboy", "zombiezombossmechdark", "zombiezombossmechegypt",
      "zombiezombossmechpirate"));

  @BeforeAll
  static void useRealFiles() {
    if (Gdx.files == null) {
      Gdx.files = new Lwjgl3Files();
    }
  }

  @Test
  void everyOrdinaryZombieCarriesTheGibsTheRendererAsksFor() throws IOException {
    Set<String> withoutHead = new TreeSet<>();
    Set<String> withoutArm = new TreeSet<>();
    for (String rig : rigNames()) {
      Set<String> parts = partsOf(rig);
      if (parts.isEmpty()) {
        continue;
      }
      if (!hasPart(parts, BodyParts.HEAD_GIB)) {
        withoutHead.add(rig);
      }
      if (!hasPart(parts, BodyParts.ARM_GIB)) {
        withoutArm.add(rig);
      }
    }
    assertEquals(NO_HEAD_GIB, withoutHead, "rigs with no " + BodyParts.HEAD_GIB);
    assertEquals(NO_ARM_GIB, withoutArm, "rigs with no " + BodyParts.ARM_GIB);
  }

  /**
   * Throwing the gib is only half of it: the body's own head is hidden at the same moment, and a
   * rig whose head no part name matches would collapse still wearing it while a second head flew
   * off. Both halves have to agree, per rig.
   */
  @Test
  void everyRigThatThrowsAHeadHasOneToHide() throws IOException {
    List<String> mismatched = new ArrayList<>();
    for (String rig : rigNames()) {
      Set<String> parts = partsOf(rig);
      if (parts.isEmpty() || !hasPart(parts, BodyParts.HEAD_GIB)) {
        continue;
      }
      if (parts.stream().noneMatch(part -> BodyParts.isHeadPart(part.toLowerCase()))) {
        mismatched.add(rig);
      }
    }
    assertTrue(mismatched.isEmpty(), "rigs that lose a head but have none to hide: " + mismatched);
  }

  @Test
  void everyRigThatThrowsAnArmHasOneToHide() throws IOException {
    List<String> mismatched = new ArrayList<>();
    for (String rig : rigNames()) {
      Set<String> parts = partsOf(rig);
      if (parts.isEmpty() || !hasPart(parts, BodyParts.ARM_GIB)) {
        continue;
      }
      if (parts.stream().noneMatch(part -> BodyParts.isOuterArmPart(part.toLowerCase()))) {
        mismatched.add(rig);
      }
    }
    assertTrue(mismatched.isEmpty(), "rigs that lose an arm but have none to hide: " + mismatched);
  }

  /**
   * Losing a head must not take the zombie with it.
   *
   * <p>The names are matched by substring and a part forced hidden takes its children with it, so
   * a match that is too greedy does not look wrong, it makes the corpse disappear -- and a death
   * that draws nothing at all is indistinguishable from the bug this work set out to fix. Every
   * rig must keep a torso and its legs.
   */
  @Test
  void hidingThePiecesThatCameOffLeavesTheRestOfTheZombie() throws IOException {
    List<String> overreaching = new ArrayList<>();
    for (String rig : rigNames()) {
      Set<String> parts = partsOf(rig);
      if (parts.isEmpty() || !hasPart(parts, BodyParts.HEAD_GIB)) {
        continue;
      }
      long hidden = parts.stream()
          .filter(part -> BodyParts.isHeadPart(part.toLowerCase())
              || BodyParts.isOuterArmPart(part.toLowerCase()))
          .count();
      if (hidden * 2 >= parts.size()) {
        overreaching.add(rig + " hides " + hidden + " of " + parts.size());
      }
      for (String core : List.of("torso", "leg", "foot")) {
        boolean has = parts.stream().anyMatch(part -> part.toLowerCase().contains(core));
        boolean kept = parts.stream()
            .filter(part -> part.toLowerCase().contains(core))
            .anyMatch(part -> !BodyParts.isHeadPart(part.toLowerCase())
                && !BodyParts.isOuterArmPart(part.toLowerCase()));
        if (has && !kept) {
          overreaching.add(rig + " would hide every " + core);
        }
      }
    }
    assertTrue(overreaching.isEmpty(), String.join(", ", overreaching));
  }

  /** The piece in the air must never be one of the parts hidden on the body, or it vanishes. */
  @Test
  void aGibIsNeverMistakenForTheBodyItLeft() {
    assertFalse(BodyParts.isHeadPart(BodyParts.HEAD_GIB), "the head gib is not the body's head");
    assertFalse(BodyParts.isOuterArmPart(BodyParts.ARM_GIB), "the arm gib is not the body's arm");
    // "armor" contains "arm", which is the trap this rules out.
    assertFalse(BodyParts.isOuterArmPart("zombie_armor_cone_norm"), "armour is not an arm");
  }

  /**
   * Every armour a rig wears has a piece to drop, and never the wrapper: drawn on its own, a
   * "_states" part is all of that armour's damage stages stacked on top of each other.
   */
  @Test
  void everyArmourInARigHasASinglePieceToThrow() throws IOException {
    List<String> broken = new ArrayList<>();
    for (String rig : rigNames()) {
      Set<String> parts = partsOf(rig);
      for (String group : List.of(ArmourParts.CONE, ArmourParts.BUCKET, ArmourParts.BRICK,
          ArmourParts.CROWN, ArmourParts.SHOULDER)) {
        boolean wears = parts.stream()
            .anyMatch(part -> group.equals(ArmourParts.groupOf(part.toLowerCase())));
        if (!wears) {
          continue;
        }
        String[] states = BodyParts.armourStateParts(parts, group);
        if (states.length == 0) {
          broken.add(rig + "/" + group + " has no piece");
          continue;
        }
        for (String state : states) {
          if (state.toLowerCase().contains("states")) {
            broken.add(rig + "/" + group + " would throw the wrapper " + state);
          }
        }
      }
    }
    assertTrue(broken.isEmpty(), String.join(", ", broken));
  }

  private static boolean hasPart(Set<String> parts, String wanted) {
    return parts.stream().anyMatch(part -> part.equalsIgnoreCase(wanted)
        || part.toLowerCase().contains(wanted.toLowerCase()));
  }

  private static List<String> rigNames() throws IOException {
    List<String> names = new ArrayList<>();
    try (DirectoryStream<Path> rigs = Files.newDirectoryStream(ZOMBIES, "*.json")) {
      for (Path rig : rigs) {
        names.add(rig.getFileName().toString().replace(".json", ""));
      }
    }
    names.sort(String::compareTo);
    return names;
  }

  /**
   * Every name a part could be given, gathered the way the baker names them: the layer's own name
   * if it has one, else the sprite it plays, else the leaf of the image it draws.
   */
  private static Set<String> partsOf(String rig) {
    AnimationManifest manifest = AnimationManifest.load("zombies", rig);
    if (manifest == null || !manifest.pam().exists()) {
      return Set.of();
    }
    PamFile pam = PamFile.read(manifest.pam());
    if (pam == null) {
      return Set.of();
    }
    Set<String> names = new LinkedHashSet<>();
    for (PamFile.Image image : pam.images) {
      add(names, image.leaf);
    }
    for (PamFile.Sprite sprite : pam.sprites) {
      add(names, sprite.name);
      for (PamFile.Frame frame : sprite.frames) {
        for (PamFile.Add append : frame.appends) {
          add(names, append.name);
        }
      }
    }
    return names;
  }

  private static void add(Set<String> names, String name) {
    if (name != null && !name.isEmpty()) {
      names.add(name);
    }
  }
}
