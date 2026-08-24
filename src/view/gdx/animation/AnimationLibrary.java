package view.gdx.animation;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and caches one {@link EntityAnimation} per plant or zombie.
 *
 * <p>Three files have to line up for an entity to animate: the manifest in
 * {@code assets/animations}, the atlas it names under {@code assets/textures}, and the PAM it
 * names under {@code resources/raw}. Not every entity has all three -- the extractor only pulled
 * the ones the game uses, and a few of those have art but no rig -- so a miss is normal and comes
 * back as null for the caller to fall back on a still portrait.
 *
 * <p>Misses are cached too, otherwise every frame would go looking for the same absent files.
 */
public final class AnimationLibrary implements Disposable {

  public static final String PLANTS = "plants";
  public static final String ZOMBIES = "zombies";

  private final Map<String, EntityAnimation> cache = new HashMap<>();
  private final List<TextureAtlas> atlases = new ArrayList<>();

  /** The animation for this entity's name, or null if it has none. */
  public EntityAnimation find(String kind, String entityName) {
    String key = kind + "/" + normalise(entityName);
    if (cache.containsKey(key)) {
      return cache.get(key);
    }
    EntityAnimation animation = load(kind, normalise(entityName));
    cache.put(key, animation);
    return animation;
  }

  private EntityAnimation load(String kind, String key) {
    if (key.isEmpty()) {
      return null;
    }
    AnimationManifest manifest = AnimationManifest.load(kind, key);
    if (manifest == null || !manifest.atlas().exists()) {
      return null;
    }
    PamFile pam = PamFile.read(manifest.pam());
    if (pam == null || pam.mainSprite == null) {
      return null;
    }
    TextureAtlas atlas = new TextureAtlas(manifest.atlas());
    atlases.add(atlas);
    EntityAnimation animation = new EntityAnimation(pam, atlas, manifest.clips());
    return animation.isUsable() ? animation : null;
  }

  /** Same key PlantArt and ZombieArt use, so one name resolves art and animation alike. */
  private static String normalise(String name) {
    return name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  @Override
  public void dispose() {
    for (TextureAtlas atlas : atlases) {
      atlas.dispose();
    }
    atlases.clear();
    cache.clear();
  }
}
