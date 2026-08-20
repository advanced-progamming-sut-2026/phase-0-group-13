package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Zombie art lookup, one atlas per zombie under {@code textures/zombies/<name>.atlas}.
 *
 * <p>Unlike {@link PlantArt} there is no single "portrait" atlas here: only some zombies have
 * art at all (15 at the time of writing, out of 40+ zombie templates), and the atlases that do
 * exist are disassembled skeletal-rig exports -- lots of small named body-part regions (e.g.
 * {@code egypt_gargantuar_102x79}) plus a handful of {@code ikNode_*} bone markers, not one
 * finished stand-alone sprite. There's no metadata (unlike plants.json's seed-packet convention)
 * saying which region, if any, reads as a clean whole-body portrait.
 *
 * <p>So this is a placeholder rather than real art: it picks the single largest non-bone region
 * in the atlas as a stand-in body part, on the theory that the largest piece is probably the
 * torso or a full-body chunk. Zombies with no atlas, or with only bone-marker regions, return
 * null and EntityRenderer falls back to its outline-plus-health-bar drawing -- see
 * EntityRenderer.drawShapes()'s "no verified portrait" branch. Wiring up the real per-frame
 * animations (walk/eat/idle) from the JSON files in assets/animations/zombies is separate,
 * later work; this class only unblocks the build and gives *something* visible for the zombies
 * that have any art at all.
 */
public final class ZombieArt implements Disposable {

  private static final String ATLAS_DIR = "textures/zombies/";

  private final Map<String, TextureAtlas> loaded = new HashMap<>();
  private final Map<String, TextureRegion> resolved = new HashMap<>();

  /** Best-effort portrait for this zombie's raw name, or null if there is none usable. */
  public TextureRegion find(String zombieName) {
    String key = normalise(zombieName);
    if (key.isEmpty()) {
      return null;
    }
    if (resolved.containsKey(key)) {
      return resolved.get(key);
    }

    TextureRegion picked = pickPortrait(key);
    resolved.put(key, picked);
    return picked;
  }

  private TextureRegion pickPortrait(String key) {
    String path = ATLAS_DIR + key + ".atlas";
    if (!Gdx.files.internal(path).exists()) {
      return null;
    }
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(path));
    loaded.put(key, atlas);

    TextureRegion best = null;
    long bestArea = -1;
    for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
      if (isBoneMarker(region.name)) {
        continue;
      }
      long area = (long) region.getRegionWidth() * region.getRegionHeight();
      if (area > bestArea) {
        bestArea = area;
        best = region;
      }
    }
    return best;
  }

  private static boolean isBoneMarker(String regionName) {
    return regionName != null && regionName.toLowerCase().startsWith("iknode");
  }

  private static String normalise(String name) {
    return name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  @Override
  public void dispose() {
    List<TextureAtlas> atlases = new ArrayList<>(loaded.values());
    for (TextureAtlas atlas : atlases) {
      atlas.dispose();
    }
    loaded.clear();
    resolved.clear();
  }
}
