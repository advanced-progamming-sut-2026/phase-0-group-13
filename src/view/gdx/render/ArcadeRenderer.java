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
import view.gdx.core.GdxConfig;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.ZombieArt;

public final class ArcadeRenderer implements Disposable {

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

  public record Look(String rig, String armour, String portrait) {}

  private static final Map<String, Look> LOOKS = looks();

  private static Map<String, Look> looks() {
    Map<String, Look> map = new LinkedHashMap<>();
    map.put("basic", new Look("ZombieTutorialDefault", null, "basic"));
    map.put("conehead", new Look("ZombieTutorialDefault", ArmourParts.CONE, "conehead"));
    map.put("buckethead", new Look("ZombieTutorialDefault", ArmourParts.BUCKET, "buckethead"));
    map.put("screen-door", new Look("ZombieTutorialDefault", ArmourParts.BRICK, "screen-door"));
    map.put("newspaper", new Look("ZombieModernNewspaperDefault", null, "newspaper"));
    map.put("football", new Look("ZombieModernAllStarDefault", null, "football"));
    map.put("gargantuar", new Look("ZombieTutorialGargantuar", null, "gargantuar"));
    map.put("sun-imp", new Look("ZombieTutorialImp", null, "sun-imp"));
    map.put("pole-vaulter",
            new Look("ZombieDarkJugglerDefault", null, "ZombieDarkJugglerDefault"));
    map.put("digger", new Look("ZombieTombRaiserDefault", null, "ZombieTombRaiserDefault"));
    map.put("ladder",
            new Look("ZombieDarkBarrelRollerDefault", null, "ZombieDarkBarrelRollerDefault"));
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

  public void beginFrame(float frameDelta) {
    this.delta = Math.max(0f, frameDelta);
  }

  public void endFrame() {
    playback.endFrame();
  }

  public static Look lookOf(String engineName) {
    return LOOKS.get(engineName == null ? "" : engineName.toLowerCase().trim());
  }

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

  public boolean drawPlant(Batch batch, Object key, String plantName, double col, int row) {
    return drawPlant(batch, key, plantName, col, row, -1, 0f);
  }

  /**
   * Same, for a plant that shoots.
   *
   * @param ticksToShot ticks until its next shot, or -1 for one that does not shoot: its attack
   *     clip is played across the ticks leading up to that so it finishes on the shot rather than
   *     the plant standing in its idle while peas come out of it
   * @param tickAlpha how far through the current tick this frame is
   */
  public boolean drawPlant(Batch batch, Object key, String plantName, double col, int row,
      int ticksToShot, float tickAlpha) {
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plantName);
    String resting = animation == null ? null : animation.pickClip("idle", "attack");
    if (resting != null) {
      String attack = animation.pickClip("attack");
      float windUp = attack == null ? -1f : windUpTime(animation, attack, ticksToShot, tickAlpha);
      String clip = windUp >= 0f ? attack : resting;
      animation.draw(batch, clip,
          windUp >= 0f ? playback.hold(key, clip, windUp) : playback.advance(key, clip, delta),
          geometry.columnCentreX(col),
          geometry.rowToY(row) + geometry.getCellHeight() * PLANT_FOOT_INSET,
          // Sized by the resting clip either way, so the plant does not change size as it shoots.
          scaleFor(animation.width(resting), PLANT_ANIM_UNITS, PLANT_ROW_FILL), false);
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

  /** Where in its attack clip a plant this close to firing is, or -1 for one that is not. */
  private float windUpTime(EntityAnimation animation, String attack, int ticksToShot,
      float tickAlpha) {
    float length = animation.duration(attack);
    if (ticksToShot < 0 || length <= 0f) {
      return -1f;
    }
    int windUp = Math.max(1, Math.round(length * GdxConfig.TICKS_PER_SECOND));
    float left = ticksToShot - tickAlpha;
    return left < 0f || left > windUp ? -1f : (1f - left / windUp) * length;
  }

  /**
   * Plays an entity's rig loose on the board as a sticker, centred on a cell rather than standing
   * in it.
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
        geometry.rowCentreY(row) - drawnHeight * scale / 2f,
        scale, false);
    return true;
  }

  public void drawProp(Batch batch, TextureRegion region, double col, int row, float rowFraction) {
    drawProp(batch, region, col, (double) row, rowFraction);
  }

  /** Same, for a prop that is between lanes -- a bowling nut on its way across a bounce. */
  public void drawProp(Batch batch, TextureRegion region, double col, double row,
      float rowFraction) {
    if (region == null) {
      return;
    }
    float height = geometry.getCellHeight() * rowFraction;
    float width = region.getRegionWidth() * height / region.getRegionHeight();
    batch.draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowCentreY(row) - height / 2f, width, height);
  }

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
