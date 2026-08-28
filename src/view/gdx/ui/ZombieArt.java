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
 * Zombie art lookup: the zombie-packet portraits first, the per-zombie atlas second.
 *
 * <p>{@code textures/zombies/zombiepackets.atlas} is the zombie equivalent of the seed-packet
 * page {@link PlantArt} reads: one finished, whole-body picture per zombie, keyed by the alias
 * Zombies.json uses. Every region in it was cropped and looked at before being listed in
 * ZOMBIE_PACKETS in tools/asset-extract/extract_assets.py, so a hit here is verified art for
 * that exact zombie.
 *
 * <p>The per-zombie atlases under {@code textures/zombies/<name>.atlas} are disassembled
 * skeletal-rig exports -- lots of small named body-part regions (e.g. {@code
 * egypt_gargantuar_102x79}) plus {@code ikNode_*} bone markers, not one finished sprite. They
 * stay as a fallback (largest non-bone region) for the few zombies with a rig but no packet, so
 * nothing that used to draw stops drawing. Zombies with neither return null and EntityRenderer
 * falls back to its outline-plus-health-bar drawing -- see the "no verified portrait" branch in
 * EntityRenderer.drawShapes(). Wiring up the real per-frame animations (walk/eat/idle) from the
 * JSON files in assets/animations/zombies is separate, later work.
 *
 * <p>The four Zombotany zombies are the one place a plant's art is used for a zombie. They have
 * no art of their own anywhere in the upstream package -- RESOURCES.json carries no 768 folder
 * for them, so the extractor lists all four under "unresolved" in assets/metadata/asset-map.json
 * and neither a packet nor a rig atlas exists. They are plant-headed zombies, so the plant they
 * are named after is the closest real asset the project has, and every one of the four has a
 * seed packet. See {@link #zombotanyPlant}.
 */
public final class ZombieArt implements Disposable {

  private static final String ATLAS_DIR = "textures/zombies/";
  private static final String PACKETS_PATH = ATLAS_DIR + "zombiepackets.atlas";

  /** The plants the four Zombotany zombies borrow their picture from, longest name first. */
  private static final List<String> ZOMBOTANY_PLANTS =
      List.of("peashooter", "jalapeno", "wallnut", "squash");

  private final Map<String, TextureAtlas> loaded = new HashMap<>();
  private final Map<String, TextureRegion> resolved = new HashMap<>();
  private Map<String, TextureRegion> packets;
  private TextureAtlas packetAtlas;
  private PlantArt plantArt;

  /** Best-effort portrait for this zombie's raw name, or null if there is none usable. */
  public TextureRegion find(String zombieName) {
    String key = normalise(zombieName);
    if (key.isEmpty()) {
      return null;
    }
    if (resolved.containsKey(key)) {
      return resolved.get(key);
    }

    TextureRegion picked = packets().get(key);
    if (picked == null) {
      picked = pickPortrait(key);
    }
    if (picked == null) {
      String plant = zombotanyPlant(key);
      if (plant != null) {
        picked = plantArt().find(plant);
      }
    }
    resolved.put(key, picked);
    return picked;
  }

  /**
   * The seed-packet name a Zombotany zombie borrows, or null when this is not one of the four.
   *
   * <p>Normalises first, so it matches the plant inside aliases like {@code
   * ZombieZombotanyWallnutDefault}. The four names it returns are exactly the four regions
   * {@code textures/plants/seedpackets.atlas} carries for those plants.
   */
  static String zombotanyPlant(String zombieName) {
    String key = normalise(zombieName);
    if (!key.contains("zombotany")) {
      return null;
    }
    for (String plant : ZOMBOTANY_PLANTS) {
      if (key.contains(plant)) {
        return plant;
      }
    }
    return null;
  }

  private PlantArt plantArt() {
    if (plantArt == null) {
      plantArt = new PlantArt();
    }
    return plantArt;
  }

  /**
   * The packet page, indexed by the same normalised key {@link #find} is asked for.
   *
   * <p>Region names on the page keep their upstream casing ({@code ZombieDarkKing}), so they are
   * normalised on the way into the map instead of being looked up directly.
   */
  private Map<String, TextureRegion> packets() {
    if (packets != null) {
      return packets;
    }
    packets = new HashMap<>();
    if (!Gdx.files.internal(PACKETS_PATH).exists()) {
      return packets;
    }
    packetAtlas = new TextureAtlas(Gdx.files.internal(PACKETS_PATH));
    for (TextureAtlas.AtlasRegion region : packetAtlas.getRegions()) {
      packets.putIfAbsent(normalise(region.name), region);
    }
    return packets;
  }

  /**
   * One region of a zombie's own rig atlas, by its upstream name.
   *
   * <p>{@link #find} wants a whole-body portrait, but a rig also carries the props a zombie brings
   * with it, and some of those outlive the zombie on screen: the beach thrower's octopus stays
   * clamped to whatever plant it landed on. Goes through the same atlas cache find() fills, so a
   * part costs nothing once that zombie's atlas is open, and it is disposed with the rest.
   *
   * @return the region, or null when the zombie has no atlas or no such region in it
   */
  public TextureRegion findPart(String zombieName, String regionName) {
    String key = normalise(zombieName);
    if (key.isEmpty() || regionName == null) {
      return null;
    }
    TextureAtlas atlas = loaded.get(key);
    if (atlas == null) {
      String path = ATLAS_DIR + key + ".atlas";
      if (!Gdx.files.internal(path).exists()) {
        return null;
      }
      atlas = new TextureAtlas(Gdx.files.internal(path));
      loaded.put(key, atlas);
    }
    return atlas.findRegion(regionName);
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
    if (packetAtlas != null) {
      packetAtlas.dispose();
      packetAtlas = null;
    }
    if (plantArt != null) {
      plantArt.dispose();
      plantArt = null;
    }
    packets = null;
  }
}
