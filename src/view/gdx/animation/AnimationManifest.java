package view.gdx.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One entry from {@code assets/animations/{plants,zombies}/<entity>.json}.
 *
 * <p>The manifests are the index the asset extractor wrote: for each entity they name the atlas
 * holding its part images, the PAM holding its timelines, and every clip in that PAM with its
 * duration in seconds. Playback speed comes from those durations rather than from anything
 * hard-coded here.
 */
final class AnimationManifest {

  private static final String MANIFEST_DIR = "animations/";
  /** Where the extractor left the PAMs. Tracked, but not on the classpath: it is 840 MB. */
  private static final String PAM_DIR = "resources/raw/pvz2/IMAGES/";

  private final String atlasPath;
  private final String pamPath;
  private final Map<String, Float> clips;

  private AnimationManifest(String atlasPath, String pamPath, Map<String, Float> clips) {
    this.atlasPath = atlasPath;
    this.pamPath = pamPath;
    this.clips = clips;
  }

  /**
   * Loads the manifest for an entity, or returns null if it has none.
   *
   * @param kind {@code "plants"} or {@code "zombies"}
   * @param key the entity name with punctuation and case stripped, e.g. {@code wallnut}
   */
  static AnimationManifest load(String kind, String key) {
    FileHandle file = Gdx.files.internal(MANIFEST_DIR + kind + "/" + key + ".json");
    if (!file.exists()) {
      return null;
    }
    JsonObject root = JsonParser.parseString(file.readString("UTF-8")).getAsJsonObject();
    JsonArray animations = root.getAsJsonArray("animations");
    if (animations == null || animations.isEmpty()) {
      return null;
    }
    JsonObject first = animations.get(0).getAsJsonObject();
    if (!first.has("pam") || !root.has("atlas")) {
      return null;
    }
    Map<String, Float> clips = new LinkedHashMap<>();
    JsonObject listed = first.getAsJsonObject("clips");
    if (listed != null) {
      for (Map.Entry<String, JsonElement> clip : listed.entrySet()) {
        clips.put(clip.getKey(), clip.getValue().getAsFloat());
      }
    }
    return new AnimationManifest(root.get("atlas").getAsString(), first.get("pam").getAsString(),
        Collections.unmodifiableMap(clips));
  }

  FileHandle atlas() {
    return Gdx.files.internal(atlasPath);
  }

  FileHandle pam() {
    return Gdx.files.internal(PAM_DIR + pamPath);
  }

  /** Seconds the clip should take, or null if this entity has no such clip. */
  Float duration(String clip) {
    return clips.get(clip);
  }

  Map<String, Float> clips() {
    return clips;
  }
}
