package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.game.Projectile;

/**
 * Which sprite a projectile flies as.
 *
 * <p>Phase Two asks for the projectile types to be told apart on sight, and the art is already
 * here: every lobber's melon, cabbage and pepper, and every shooter's pea, sit as parts in that
 * plant's own atlas under {@code textures/plants}. So each kind names an atlas and a region
 * instead of everything sharing the one HUD pea.
 *
 * <p>The kind comes off the projectile itself -- arcing, splash, piercing, elemental effect --
 * which is what the model already records, so nothing had to be tagged for the view's benefit.
 * Anything that cannot be resolved falls back to the pea, which is where it started.
 */
public final class ProjectileArt implements Disposable {

  /** One projectile sprite: the art, its height as a fraction of a lane, and its facing. */
  public record Shot(TextureRegion region, float rowFraction, float angle) {}

  private static final String HUD_ATLAS = "textures/ui/hud.atlas";
  private static final String PLANT_ATLAS = "textures/plants/";

  private static final float PEA_FILL = 0.22f;
  private static final float ZOMBIE_PEA_FILL = 0.28f;
  private static final float MELON_FILL = 0.32f;
  private static final float CABBAGE_FILL = 0.26f;
  private static final float PEPPER_FILL = 0.28f;
  private static final float CLOUD_FILL = 0.36f;
  // The spike is stored pointing up, so this is its length once it is turned to fly flat.
  private static final float SPIKE_FILL = 0.34f;

  private final Map<String, TextureAtlas> atlases = new HashMap<>();
  private final List<TextureAtlas> loaded = new ArrayList<>();

  /** The sprite for this shot, or null if even the pea is missing. */
  public Shot find(Projectile projectile) {
    if (projectile.isFromZombie()) {
      // reflected and zombie-fired shots stay peas: the caller tints them so they read as wrong
      return pea(ZOMBIE_PEA_FILL);
    }
    if (projectile.isLobbed()) {
      return lobbed(projectile);
    }
    if (projectile.isPiercing()) {
      // Cactus is the one with a pass limit; Fume-shroom's cloud goes through everything
      return projectile.getPierceLimit() > 0
          ? shot("cactus", "cactus_21x65", SPIKE_FILL, -90f)
          : shot("fumeshroom", "fumeshroom_48x44", CLOUD_FILL, 0f);
    }
    return switch (projectile.getEffect()) {
      case FIRE -> shot("firepeashooter", "firepeashooter_33x35", PEA_FILL, 0f);
      case ICE -> shot("snowpea", "snowpea_33x35", PEA_FILL, 0f);
      case POISON -> shot("goopeashooter", "goopeashooter_31x33", PEA_FILL, 0f);
      default -> pea(PEA_FILL);
    };
  }

  private Shot lobbed(Projectile projectile) {
    return switch (projectile.getEffect()) {
      case ICE -> shot("wintermelon", "wintermelon_122x83", MELON_FILL, 0f);
      case FIRE -> shot("pepperpult", "pepperpult_55x61", PEPPER_FILL, 0f);
      // splash is what separates Melon-pult's watermelon from Cabbage-pult's cabbage
      default -> projectile.getSplashRadius() > 0
          ? shot("melonpult", "Melonpult_122x83", MELON_FILL, 0f)
          : shot("cabbagepult", "cabbagepult_107x107", CABBAGE_FILL, 0f);
    };
  }

  private Shot shot(String plant, String region, float rowFraction, float angle) {
    TextureAtlas atlas = atlas(PLANT_ATLAS + plant + ".atlas");
    TextureRegion art = atlas == null ? null : atlas.findRegion(region);
    return art == null ? pea(rowFraction) : new Shot(art, rowFraction, angle);
  }

  private Shot pea(float rowFraction) {
    TextureAtlas hud = atlas(HUD_ATLAS);
    TextureRegion art = hud == null ? null : hud.findRegion("pea");
    return art == null ? null : new Shot(art, rowFraction, 0f);
  }

  private TextureAtlas atlas(String path) {
    if (atlases.containsKey(path)) {
      return atlases.get(path);
    }
    TextureAtlas atlas = null;
    if (Gdx.files.internal(path).exists()) {
      atlas = new TextureAtlas(Gdx.files.internal(path));
      loaded.add(atlas);
    }
    atlases.put(path, atlas);
    return atlas;
  }

  @Override
  public void dispose() {
    for (TextureAtlas atlas : loaded) {
      atlas.dispose();
    }
    loaded.clear();
    atlases.clear();
  }
}
