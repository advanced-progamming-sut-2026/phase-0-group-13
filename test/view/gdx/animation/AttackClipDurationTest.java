package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import view.gdx.core.GdxConfig;

/**
 * EntityRenderer holds a plant's attack pose for as long as the clip actually runs, read off the
 * rig at draw time. It used to hold a flat four ticks instead, which cut every attack off part-way
 * through and snapped it back to idle: a Peashooter played 39% of its shot, a Cabbage-pult 24% of
 * its throw, and the Repeater -- whose clip is one volley of two peas -- never got as far as the
 * second one. That was the report: "the animation for repeater isn't ok".
 *
 * <p>Reads the manifests straight off disk rather than through AnimationLibrary, which needs a GL
 * context for the atlas. The durations here are the same numbers AnimationManifest hands to
 * EntityAnimation, so this checks the data the fix depends on.
 */
class AttackClipDurationTest {

  private static final Path MANIFESTS = Path.of("assets", "animations", "plants");

  /** The old fixed hold, in ticks. Kept as the number this is arguing against. */
  private static final int OLD_FIXED_HOLD_TICKS = 4;

  /**
   * The range the clips actually span, which is why EntityRenderer caps the hold at nothing and
   * just uses the rig's own number: a third of a second at one end and four and a half at the
   * other, each of them a real length for what that plant is doing. A clip outside this range is
   * worth a look before it is trusted -- it would hold a plant in its attack pose for that long.
   */
  private static final float SHORTEST_SANE_ATTACK_SECONDS = 0.2f;
  private static final float LONGEST_SANE_ATTACK_SECONDS = 5f;

  @Test
  void theRepeaterHasAnAttackClipLongerThanTheOldFixedHold() throws IOException {
    Float attack = attackSeconds("repeater");
    assertTrue(attack != null, "the repeater manifest should list an attack clip");
    float oldHoldSeconds = OLD_FIXED_HOLD_TICKS / (float) GdxConfig.TICKS_PER_SECOND;
    assertTrue(attack > oldHoldSeconds,
        "the repeater's attack runs " + attack + "s, so a flat " + oldHoldSeconds
            + "s hold could only ever show the first part of the volley");
  }

  /**
   * The reason the fix is worth having at all: this was never a repeater-only problem, it was every
   * shooter in the game losing most of its attack.
   */
  @Test
  void mostAttackClipsAreLongerThanTheOldFixedHold() throws IOException {
    float oldHoldSeconds = OLD_FIXED_HOLD_TICKS / (float) GdxConfig.TICKS_PER_SECOND;
    List<String> truncated = new ArrayList<>();
    int withAttack = 0;
    for (Map.Entry<String, Float> entry : allAttackClips().entrySet()) {
      withAttack++;
      if (entry.getValue() > oldHoldSeconds) {
        truncated.add(entry.getKey());
      }
    }
    assertTrue(withAttack > 0, "no plant manifest listed an attack clip; the data moved");
    assertTrue(truncated.size() > withAttack / 2,
        "expected most attack clips to outlast the old " + oldHoldSeconds + "s hold, "
            + "which is what made the flat number the wrong approach; "
            + truncated.size() + " of " + withAttack + " did");
  }

  /**
   * The hold is uncapped, so a clip's listed duration goes straight through to how long a plant
   * stands in its attack pose. Nothing in the shipped manifests is absurd; this is here so a bad
   * number in a future manifest is caught as data rather than showing up as a plant frozen
   * mid-attack for a quarter of a minute.
   */
  @Test
  void everyAttackClipHasAPlausibleDuration() throws IOException {
    for (Map.Entry<String, Float> entry : allAttackClips().entrySet()) {
      float seconds = entry.getValue();
      assertTrue(seconds >= SHORTEST_SANE_ATTACK_SECONDS && seconds <= LONGEST_SANE_ATTACK_SECONDS,
          entry.getKey() + "'s attack is listed as " + seconds + "s, outside the "
              + SHORTEST_SANE_ATTACK_SECONDS + "-" + LONGEST_SANE_ATTACK_SECONDS
              + "s the rest of the roster sits in; EntityRenderer holds the pose for exactly this "
              + "long, so check the manifest before trusting it");
    }
  }

  private static Float attackSeconds(String plant) throws IOException {
    return allAttackClips().get(plant);
  }

  /** Every plant manifest that lists an attack clip, keyed by the manifest's own file name. */
  private static Map<String, Float> allAttackClips() throws IOException {
    Map<String, Float> found = new java.util.LinkedHashMap<>();
    if (!Files.isDirectory(MANIFESTS)) {
      return found;
    }
    try (var files = Files.list(MANIFESTS)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        var animations = root.getAsJsonArray("animations");
        if (animations == null || animations.isEmpty()) {
          continue;
        }
        JsonObject clips = animations.get(0).getAsJsonObject().getAsJsonObject("clips");
        if (clips != null && clips.has("attack")) {
          String name = file.getFileName().toString().replace(".json", "");
          found.put(name, clips.get("attack").getAsFloat());
        }
      }
    }
    return found;
  }
}
