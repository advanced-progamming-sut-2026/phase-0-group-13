package view.gdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import java.util.LinkedHashMap;
import java.util.Map;
import view.gdx.animation.AnimationLibrary;
import view.gdx.animation.AnimationStates;
import view.gdx.animation.EntityAnimation;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.ZombieArt;

/**
 * Draws the arcade mini-games' entities on a lawn.
 *
 * <p>{@link EntityRenderer} draws a {@link model.game.Board}: it takes {@code Plant} and
 * {@code Zombie} objects and reads their armour, status effects and behaviours. The three arcade
 * engines have none of that -- they keep their own small entities and describe them by name -- so
 * this is the same art pipeline reached by name instead. Everything underneath is shared:
 * {@link AnimationLibrary} for the rigs, {@link PlantArt} and {@link ZombieArt} for the portraits,
 * {@link HudArt} for the props, and {@link ArmourParts} for the armour a rig carries.
 *
 * <p>Nothing here decides anything about a mini-game. It is told a name and a cell and it draws.
 */
public final class ArcadeRenderer implements Disposable {

  // Same references EntityRenderer uses, so a mini-game zombie is the size a zombie is: rigs are
  // measured in PAM units off a shared canvas, portraits in pixels.
  private static final float PLANT_ANIM_UNITS = 90f;
  private static final float ZOMBIE_ANIM_UNITS = 150f;
  private static final float PLANT_REFERENCE_HEIGHT = 70f;
  private static final float ZOMBIE_REFERENCE_HEIGHT = 104f;
  private static final float PLANT_ROW_FILL = 0.78f;
  private static final float ZOMBIE_ROW_FILL = 0.92f;
  private static final float PLANT_FOOT_INSET = 0.14f;
  private static final float ZOMBIE_FOOT_INSET = 0.08f;

  private static final Color HEALTH_BACK = new Color(0f, 0f, 0f, 0.55f);
  private static final Color HEALTH_FRONT = new Color(0.25f, 0.85f, 0.3f, 0.95f);
  private static final Color HEALTH_LOW = new Color(0.9f, 0.5f, 0.15f, 0.95f);

  /** How a mini-game zombie is drawn: its rig, the armour that rig should show, its portrait. */
  public record Look(String rig, String armour, String portrait) {}

  /**
   * Engine name to art.
   *
   * <p>I, Zombie deploys the classic modern-day roster and Vase Breaker and Bowling both release
   * the plain walker, so all three share one rig family: {@code ZombieTutorialDefault} carries the
   * cone, the bucket and the brick on the one skeleton, exactly as the Egypt walker does.
   *
   * <p>Three of the engine's types are missing on purpose. Pole Vaulter, Digger and Ladder are
   * Plants vs. Zombies 1 zombies and the library this project draws from has no art for any of
   * them -- no folder, no packet, no image id -- so they resolve to nothing and get the
   * no-portrait outline rather than somebody else's picture.
   */
  private static final Map<String, Look> LOOKS = looks();

  private static Map<String, Look> looks() {
    Map<String, Look> map = new LinkedHashMap<>();
    map.put("basic", new Look("ZombieTutorialDefault", null, "basic"));
    map.put("conehead", new Look("ZombieTutorialDefault", ArmourParts.CONE, "conehead"));
    map.put("buckethead", new Look("ZombieTutorialDefault", ArmourParts.BUCKET, "buckethead"));
    // The engine calls it a screen door; the library's armour-4 walker wears a brick block, in
    // this world and in every other, and that is the portrait the packet page already carries.
    map.put("screen-door", new Look("ZombieTutorialDefault", ArmourParts.BRICK, "screen-door"));
    map.put("newspaper", new Look("ZombieModernNewspaperDefault", null, "newspaper"));
    map.put("football", new Look("ZombieModernAllStarDefault", null, "football"));
    map.put("gargantuar", new Look("ZombieTutorialGargantuar", null, "gargantuar"));
    map.put("sun-imp", new Look("ZombieTutorialImp", null, "sun-imp"));
    // What Vase Breaker and Bowling call the zombie they let out.
    map.put("zombie", new Look("ZombieTutorialDefault", null, "basic"));
    return map;
  }

  private final LawnGeometry geometry;
  private final AnimationLibrary animations = new AnimationLibrary();
  private final AnimationStates playback = new AnimationStates();
  private final PlantArt plantArt = new PlantArt();
  private final ZombieArt zombieArt = new ZombieArt();
  private final HudArt hudArt = new HudArt();

  private float delta;

  public ArcadeRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  /** Call once a frame before drawing. A delta of zero freezes every rig, which is what pause is. */
  public void beginFrame(float frameDelta) {
    this.delta = Math.max(0f, frameDelta);
  }

  /** Call once a frame after drawing, so playback state for entities that died is let go. */
  public void endFrame() {
    playback.endFrame();
  }

  public static Look lookOf(String engineName) {
    return LOOKS.get(engineName == null ? "" : engineName.toLowerCase().trim());
  }

  /** Portrait for a picker card, or null when this type has no verified art. */
  public TextureRegion zombiePortrait(String engineName) {
    Look look = lookOf(engineName);
    return look == null ? null : zombieArt.find(look.portrait());
  }

  public TextureRegion plantPortrait(String plantName) {
    return plantArt.find(plantName);
  }

  public TextureRegion icon(String hudName) {
    return hudArt.find(hudName);
  }

  /**
   * Draws a mini-game zombie standing on its cell.
   *
   * @param key anything stable per entity; it is what keeps two zombies of one kind out of step
   * @return false when nothing was drawn, so the caller can outline the tile instead
   */
  public boolean drawZombie(Batch batch, Object key, String engineName, double col, int row,
      boolean eating) {
    Look look = lookOf(engineName);
    if (look == null) {
      return false;
    }
    EntityAnimation animation = animations.find(AnimationLibrary.ZOMBIES, look.rig());
    if (animation != null) {
      String clip = eating
          ? animation.pickClip("eat", "walk", "idle")
          : animation.pickClip("walk", "idle");
      if (clip != null) {
        animation.draw(batch, clip, playback.advance(key, clip, delta),
            geometry.columnCentreX(col),
            geometry.rowToY(row) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET,
            scaleFor(animation.width(clip), ZOMBIE_ANIM_UNITS, ZOMBIE_ROW_FILL),
            false, ArmourParts.wearing(animation, look.armour()));
        return true;
      }
    }
    TextureRegion portrait = zombieArt.find(look.portrait());
    if (portrait == null) {
      return false;
    }
    drawStanding(batch, portrait, col, row,
        scaleFor(portrait.getRegionWidth(), ZOMBIE_REFERENCE_HEIGHT, ZOMBIE_ROW_FILL),
        ZOMBIE_FOOT_INSET);
    return true;
  }

  /** Same for a plant, which only ever needs its idle here. */
  public boolean drawPlant(Batch batch, Object key, String plantName, double col, int row) {
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plantName);
    String clip = animation == null ? null : animation.pickClip("idle", "attack");
    if (clip != null) {
      animation.draw(batch, clip, playback.advance(key, clip, delta),
          geometry.columnCentreX(col),
          geometry.rowToY(row) + geometry.getCellHeight() * PLANT_FOOT_INSET,
          scaleFor(animation.width(clip), PLANT_ANIM_UNITS, PLANT_ROW_FILL), false);
      return true;
    }
    TextureRegion portrait = plantArt.find(plantName);
    if (portrait == null) {
      return false;
    }
    drawStanding(batch, portrait, col, row,
        scaleFor(portrait.getRegionWidth(), PLANT_REFERENCE_HEIGHT, PLANT_ROW_FILL),
        PLANT_FOOT_INSET);
    return true;
  }

  /**
   * Plays an entity's rig loose on the board as a sticker, centred on a cell rather than standing
   * in it.
   *
   * <p>The reaction stickers are the game's own rigs -- a chomper biting, a gargantuar swinging --
   * rather than new art, so they animate for the same reason everything else does and cost nothing
   * but a manifest lookup. Clips loop, so a sticker keeps moving for as long as it is shown.
   *
   * <p>Sized against the lane rather than through {@link #scaleFor}: that clamps a sprite to one
   * cell's width so board entities cannot overlap their neighbours, which is exactly the wrong
   * rule for something meant to sit over the board and be noticed.
   *
   * @param kind {@link AnimationLibrary#PLANTS} or {@link AnimationLibrary#ZOMBIES}
   * @param rig the entity whose animation to play, e.g. {@code chomper}
   * @param clip the wanted clip; falls back to the rig's idle when it has no such clip
   * @param rowFill how many lanes tall the sticker should stand
   * @return false when this rig has no animation, so the caller can show a still instead
   */
  public boolean drawSticker(Batch batch, Object key, String kind, String rig, String clip,
      double col, int row, float rowFill) {
    EntityAnimation animation = animations.find(kind, rig);
    String playing = animation == null ? null : animation.pickClip(clip, "idle");
    if (playing == null) {
      return false;
    }
    float drawnHeight = animation.height(playing);
    if (drawnHeight <= 0f) {
      return false;
    }
    float scale = geometry.getCellHeight() * rowFill / drawnHeight;
    animation.draw(batch, playing, playback.advance(key, playing, delta),
        geometry.columnCentreX(col),
        // centred on the cell, not standing on it: a sticker floats rather than occupying a tile
        geometry.rowCentreY(row) - drawnHeight * scale / 2f,
        scale, false);
    return true;
  }

  /** A flat prop -- a vase, a nut, a brain -- sized to a fraction of the lane and centred on it. */
  public void drawProp(Batch batch, TextureRegion region, double col, int row, float rowFraction) {
    if (region == null) {
      return;
    }
    float height = geometry.getCellHeight() * rowFraction;
    float width = region.getRegionWidth() * height / region.getRegionHeight();
    batch.draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowCentreY(row) - height / 2f, width, height);
  }

  /** Same, but left of the lawn: the brains and the mowers stand off the board. */
  public void drawBesideLane(Batch batch, TextureRegion region, int row, float rowFraction,
      float gap) {
    if (region == null) {
      return;
    }
    float height = geometry.getCellHeight() * rowFraction;
    float width = region.getRegionWidth() * height / region.getRegionHeight();
    batch.draw(region, geometry.columnToX(0) - width - gap,
        geometry.rowCentreY(row) - height / 2f, width, height);
  }

  /** The bar every mini-game hangs over a damaged entity. */
  public void healthBar(ShapeRenderer shapes, double col, int row, float fraction, float lift) {
    float clamped = Math.max(0f, Math.min(1f, fraction));
    float width = geometry.getCellWidth() * 0.55f;
    float x = geometry.columnCentreX(col) - width / 2f;
    float y = geometry.rowToY(row) + geometry.getCellHeight() * lift;
    shapes.setColor(HEALTH_BACK);
    shapes.rect(x - 1f, y - 1f, width + 2f, 7f);
    shapes.setColor(clamped < 0.35f ? HEALTH_LOW : HEALTH_FRONT);
    shapes.rect(x, y, width * clamped, 5f);
  }

  /** The stand-in for an entity with no verified art, same as EntityRenderer draws. */
  public void outline(ShapeRenderer shapes, double col, int row) {
    shapes.setColor(Color.WHITE);
    shapes.rect(geometry.columnCentreX(col) - geometry.getCellWidth() * 0.25f,
        geometry.rowToY(row) + geometry.getCellHeight() * 0.1f,
        geometry.getCellWidth() * 0.5f, geometry.getCellHeight() * 0.7f);
  }

  private void drawStanding(Batch batch, TextureRegion region, double col, int row, float scale,
      float footInset) {
    float width = region.getRegionWidth() * scale;
    float height = region.getRegionHeight() * scale;
    batch.draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowToY(row) + geometry.getCellHeight() * footInset, width, height);
  }

  private float scaleFor(float spriteWidth, float referenceHeight, float rowFill) {
    float scale = geometry.getCellHeight() * rowFill / referenceHeight;
    float widest = geometry.getCellWidth() * 0.95f;
    if (spriteWidth * scale > widest) {
      scale = widest / spriteWidth;
    }
    return scale;
  }

  @Override
  public void dispose() {
    animations.dispose();
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
  }
}
