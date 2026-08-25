package view.gdx.render;

import java.util.HashMap;
import java.util.Map;
import view.gdx.animation.EntityAnimation;

/**
 * Which parts of a walker rig are armour, in the rigs' own vocabulary.
 *
 * <p>The walker rigs carry their armour with them: one body, plus a cone, a bucket, a brick, a
 * crown and shoulder plates parked on the same skeleton and hidden by default. Two callers need to
 * read those names -- {@link EntityRenderer}, which switches them on from the model's {@code Armor}
 * list, and {@link ArcadeRenderer}, whose mini-game zombies have no model behind them and just say
 * which piece they wear -- so the vocabulary lives here rather than in either of them.
 */
public final class ArmourParts {

  public static final String CONE = "cone";
  public static final String BUCKET = "bucket";
  public static final String BRICK = "brick";
  public static final String CROWN = "crown";
  public static final String SHOULDER = "shoulder";

  private ArmourParts() {}

  /** The armour keyword in a part name, or null if it is body art. */
  public static String groupOf(String lowerPartName) {
    // The rigs wrap each armour's three damage states in a group named by the armour's index
    // rather than by what it is -- "_zombie_egypt_armor1_states" holds the cone. The wrapper has
    // to be switched on with its contents, because hiding a parent hides everything under it.
    // Same numbering ZombieTypeResolver reads: 1 cone, 2 bucket, 3 knight, 4 brick.
    if (lowerPartName.contains("armor1")) {
      return CONE;
    }
    if (lowerPartName.contains("armor2")) {
      return BUCKET;
    }
    if (lowerPartName.contains("armor3")) {
      return CROWN;
    }
    if (lowerPartName.contains("armor4")) {
      return BRICK;
    }
    if (lowerPartName.contains(CONE)) {
      return CONE;
    }
    if (lowerPartName.contains(BUCKET)) {
      return BUCKET;
    }
    if (lowerPartName.contains(BRICK)) {
      return BRICK;
    }
    if (lowerPartName.contains(CROWN)) {
      return CROWN;
    }
    if (lowerPartName.contains("shoulder_armor")) {
      return SHOULDER;
    }
    return null;
  }

  /** True for the two damaged states each armour is authored in, false for the whole piece. */
  public static boolean isDamagedState(String lowerPartName) {
    return lowerPartName.contains("damage_01") || lowerPartName.contains("damage_02")
        || lowerPartName.endsWith("damaged1") || lowerPartName.endsWith("damaged2");
  }

  /**
   * One armour group forced on, whole, and every other group left hidden.
   *
   * <p>For a zombie with no model {@code Armor} behind it to wear down: the arcade mini-games deal
   * in a type name, so the piece is either on or the zombie is not that type.
   *
   * @param group one of the constants here, or null for a bare zombie
   * @return a visibility map for {@link EntityAnimation#draw}, or null to keep the rig's defaults
   */
  public static Map<String, Boolean> wearing(EntityAnimation animation, String group) {
    if (animation == null || group == null) {
      return null;
    }
    Map<String, Boolean> visibility = new HashMap<>();
    boolean any = false;
    for (String part : animation.partNames()) {
      String lower = part.toLowerCase();
      if (!group.equals(groupOf(lower))) {
        continue;
      }
      boolean show = !isDamagedState(lower);
      visibility.put(part, show);
      any |= show;
    }
    return any ? visibility : null;
  }
}
