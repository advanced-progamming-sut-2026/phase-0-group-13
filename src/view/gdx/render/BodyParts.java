package view.gdx.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Which parts of a zombie rig come off it when it dies, by name.
 *
 * <p>Split out of the renderer, and public, for the same reason {@link ArmourParts} is: these are
 * upstream PopCap names matched by name, the matching is silent when it is wrong -- a name that no
 * longer matches throws no piece and hides nothing, with no error anywhere -- and the only way to
 * know it still holds across all 37 rigs is a test that reads the rigs.
 */
public final class BodyParts {

  /** PopCap's gibs. 28 of the 37 zombie rigs carry them; bosses and Gargantuars do not. */
  public static final String HEAD_GIB = "particle_head";

  public static final String ARM_GIB = "particle_arm";

  private static final Set<String> HEAD_WORDS = Set.of("skull", "jaw", "pupil", "head");

  private static final Set<String> ARM_WORDS = Set.of("arm", "arms", "hand", "hands");

  private static final Set<String> OUTER = Set.of("outer");

  private BodyParts() {}

  /**
   * The head on the body, which is hidden when the gib is thrown so there is only ever one.
   *
   * <p>Matched a word at a time and not by substring, because a rig's images are named after the
   * zombie: every image in the Crystal Skull's rig is a {@code zombie_lostcity_crystalskull_NNxNN}
   * and a plain {@code contains("skull")} hid 61 of its 103 parts -- the whole zombie, legs and
   * all -- the moment it died. Never the gib itself either: that is drawn loose, and matching it
   * here would hide the piece as soon as it came off.
   */
  public static boolean isHeadPart(String lowerPartName) {
    if (lowerPartName.contains("particle")) {
      return false;
    }
    return containsAny(lowerPartName, HEAD_WORDS);
  }

  /**
   * The outer arm only, so a zombie loses one arm rather than both.
   *
   * <p>"armor" contains "arm", which is why armour is ruled out before anything else.
   */
  public static boolean isOuterArmPart(String lowerPartName) {
    if (lowerPartName.contains("particle") || lowerPartName.contains("armor")) {
      return false;
    }
    return containsAny(lowerPartName, ARM_WORDS) && containsAny(lowerPartName, OUTER);
  }

  /** True when any whole underscore-separated word of the name is one of these. */
  private static boolean containsAny(String lowerPartName, Set<String> words) {
    for (String word : lowerPartName.split("_")) {
      if (words.contains(word)) {
        return true;
      }
    }
    return false;
  }

  /**
   * One armour's parts, most damaged first, so the piece that falls is the state it broke in.
   *
   * @param partNames every part in the rig, as {@code EntityAnimation.partNames()} gives them
   * @param group one of the {@link ArmourParts} constants
   * @return names to try in order, or an empty array when the rig has no such armour
   */
  public static String[] armourStateParts(Collection<String> partNames, String group) {
    List<String> damaged = new ArrayList<>();
    List<String> whole = new ArrayList<>();
    for (String part : partNames) {
      String lower = part.toLowerCase();
      if (!group.equals(ArmourParts.groupOf(lower))) {
        continue;
      }
      // The wrapper holding all the states is no use drawn loose: it is every state of the armour
      // stacked on top of each other.
      if (lower.contains("states")) {
        continue;
      }
      (ArmourParts.isDamagedState(lower) ? damaged : whole).add(part);
    }
    damaged.sort(Comparator.reverseOrder());
    damaged.addAll(whole);
    return damaged.toArray(new String[0]);
  }
}
