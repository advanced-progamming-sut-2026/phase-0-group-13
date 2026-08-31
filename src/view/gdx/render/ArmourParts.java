package view.gdx.render;

import java.util.HashMap;
import java.util.Map;
import view.gdx.animation.EntityAnimation;

public final class ArmourParts {

  public static final String CONE = "cone";
  public static final String BUCKET = "bucket";
  public static final String BRICK = "brick";
  public static final String CROWN = "crown";
  public static final String SHOULDER = "shoulder";

  private ArmourParts() {}

  public static String groupOf(String lowerPartName) {
    // rather than by what it is -- "_zombie_egypt_armor1_states" holds the cone. The wrapper has
    // to be switched on with its contents, because hiding a parent hides everything under it.
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
