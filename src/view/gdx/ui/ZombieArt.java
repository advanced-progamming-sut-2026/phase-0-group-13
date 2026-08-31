package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class ZombieArt implements Disposable {

  private static final String ATLAS_DIR = "textures/zombies/";
  private static final String PACKETS_PATH = ATLAS_DIR + "zombiepackets.atlas";

  private static final List<String> ZOMBOTANY_PLANTS =
      List.of("peashooter", "jalapeno", "wallnut", "squash");

  private final Map<String, TextureAtlas> loaded = new HashMap<>();
  private final Map<String, TextureRegion> resolved = new HashMap<>();
  private Map<String, TextureRegion> packets;
  private TextureAtlas packetAtlas;
  private PlantArt plantArt;

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

  public static String zombotanyPlant(String zombieName) {
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
