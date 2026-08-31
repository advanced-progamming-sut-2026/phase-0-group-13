package view.gdx.animation;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnimationLibrary implements Disposable {

  public static final String PLANTS = "plants";
  public static final String ZOMBIES = "zombies";

  private final Map<String, EntityAnimation> cache = new HashMap<>();
  private final List<TextureAtlas> atlases = new ArrayList<>();

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
