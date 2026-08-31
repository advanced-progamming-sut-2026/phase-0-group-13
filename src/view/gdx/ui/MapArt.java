package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * The adventure map's own art: the four world cards and the per-chapter map pieces.
 *
 * <p>Same lazy shape as {@link HudArt} -- the atlas opens on first use, a miss is cached, and every
 * getter returns null rather than throwing so a caller can fall back.
 */
public final class MapArt implements Disposable {

  private static final String WORLDS_PATH = "textures/ui/worldselect.atlas";
  private static final String MAP_PATH = "textures/ui/levelmap.atlas";

  /** Stage number to the prefix its regions are named with. Matches MatchLauncher's seasons. */
  private static final String[] CHAPTER_KEYS = {"egypt", "frostbite", "beach", "darkages"};

  private TextureAtlas worlds;
  private TextureAtlas map;
  private boolean worldsFailed;
  private boolean mapFailed;

  public static String chapterKey(int stage) {
    return CHAPTER_KEYS[Math.floorMod(stage - 1, CHAPTER_KEYS.length)];
  }

  /** The whole island card for a chapter, name and difficulty chillies included. */
  public TextureRegion world(int stage) {
    TextureAtlas atlas = worlds();
    return atlas == null ? null : atlas.findRegion(chapterKey(stage));
  }

  /** A map piece, e.g. {@code piece(1, "hub")} or {@code piece(1, "node_open")}. */
  public TextureRegion piece(int stage, String name) {
    TextureAtlas atlas = map();
    return atlas == null ? null : atlas.findRegion(chapterKey(stage) + "_" + name);
  }

  private TextureAtlas worlds() {
    if (worlds == null && !worldsFailed) {
      if (!Gdx.files.internal(WORLDS_PATH).exists()) {
        worldsFailed = true;
        return null;
      }
      worlds = new TextureAtlas(Gdx.files.internal(WORLDS_PATH));
    }
    return worlds;
  }

  private TextureAtlas map() {
    if (map == null && !mapFailed) {
      if (!Gdx.files.internal(MAP_PATH).exists()) {
        mapFailed = true;
        return null;
      }
      map = new TextureAtlas(Gdx.files.internal(MAP_PATH));
    }
    return map;
  }

  @Override
  public void dispose() {
    if (worlds != null) {
      worlds.dispose();
      worlds = null;
    }
    if (map != null) {
      map.dispose();
      map = null;
    }
  }
}
