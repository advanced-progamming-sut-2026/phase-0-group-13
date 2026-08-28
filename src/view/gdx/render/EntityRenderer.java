package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.core.GameManager;
import model.enums.StatusEffect;
import model.enums.SunType;
import model.game.Board;
import model.game.Projectile;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.behavior.JesterZombieAction;
import model.game.zombie.behavior.KingAuraZombieAction;
import model.game.zombie.behavior.ZombossAction;
import view.gdx.animation.AnimationLibrary;
import view.gdx.animation.AnimationStates;
import view.gdx.animation.EntityAnimation;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.ProjectileArt;
import view.gdx.ui.ZombieArt;

/** Plants, zombies, projectiles and suns on top of the lawn. */
public final class EntityRenderer implements WorldRenderer {

  // Sprites are scaled off a reference height instead of being stretched to the tile, so a
  // gargantuar stays bigger than an imp and nothing gets distorted.
  private static final float ZOMBIE_REFERENCE_HEIGHT = 104f;
  private static final float ZOMBIE_ROW_FILL = 0.92f;
  private static final float PLANT_REFERENCE_HEIGHT = 70f;
  private static final float PLANT_ROW_FILL = 0.78f;
  // Zomboss's hit box in Zombies.json is 340 tall against a walker's 95, so it is not a
  // zombie-sized sprite: it stands about two and a half lanes high and is allowed to be wide.
  private static final float ZOMBOSS_ROW_FILL = 2.4f;
  private static final float ZOMBIE_FOOT_INSET = 0.08f;

  /** Columns from the house at which a zombie starts showing the warning tint. */
  private static final double NEAR_HOUSE_COLUMN = 1.2;
  private static final float PLANT_FOOT_INSET = 0.14f;
  // The stand-in for the entities that still have no rig: a tiny bob on the footInset fraction.
  private static final float PLANT_IDLE_SPEED = 2.2f;
  private static final float PLANT_IDLE_BOB_FRACTION = 0.02f;
  // Animated art is measured in PAM units off a shared 390-unit canvas, so one reference per side
  // keeps the relative sizes the artists drew: a gargantuar stays bigger than an imp.
  private static final float PLANT_ANIM_UNITS = 90f;
  private static final float ZOMBIE_ANIM_UNITS = 150f;
  // Chilled zombies walk at half speed, so their legs have to as well.
  private static final float CHILLED_ANIM_RATE = 0.5f;
  // Fractions of an armour's health at which the rig's two damaged states take over.
  private static final float ARMOUR_STAGE_1 = 0.66f;
  private static final float ARMOUR_STAGE_2 = 0.33f;
  // How long a shooter holds its attack clip after firing. The model runs at ten ticks a
  // second, so this is a little under half a second.
  private static final int PLANT_ATTACK_HOLD_TICKS = 4;
  // How far above its lane a lobbed shot rises at the top of the arc.
  private static final float LOB_ARC_HEIGHT = 0.85f;
  private static final float FREEZE_LEVELS = Plant.MAX_FREEZE_LEVEL;
  // The octopus a beach thrower leaves on a plant. It is one prop out of that zombie's own rig --
  // the little orange one with the eyes, checked against the atlas page before being named here.
  private static final String OCTOPUS_RIG = "zombiebeachoctopus";
  private static final String OCTOPUS_REGION = "zombie_beach_octopus_66x76";
  private static final float OCTOPUS_ROW_FILL = 0.44f;
  // Clamped over the plant's head rather than centred on the tile, and off to one side so it does
  // not simply cover the face of whatever it caught.
  private static final float OCTOPUS_LIFT = 0.52f;
  private static final float OCTOPUS_NUDGE_RIGHT = 0.10f;

  private final LawnGeometry geometry;
  private final PlantArt plantArt = new PlantArt();
  private final ZombieArt zombieArt = new ZombieArt();
  private final HudArt hudArt = new HudArt();
  private final ProjectileArt projectileArt = new ProjectileArt();
  private final AnimationLibrary animations = new AnimationLibrary();
  private final AnimationStates playback = new AnimationStates();
  private final Color healthBack = new Color(0f, 0f, 0f, 0.55f);
  private final Color healthFront = new Color(0.25f, 0.85f, 0.3f, 0.95f);
  private final Color healthLow = new Color(0.9f, 0.5f, 0.15f, 0.95f);
  private final Color peaColor = new Color(0.55f, 0.9f, 0.3f, 1f);
  private final Color sunColor = new Color(1f, 0.85f, 0.2f, 1f);
  private final Color noArt = new Color(1f, 1f, 1f, 0.85f);
  private final Color frozenTint = new Color(0.45f, 0.7f, 1f, 1f);
  private final Color chilledTint = new Color(0.72f, 0.88f, 1f, 1f);
  private final Color hypnoTint = new Color(0.85f, 0.6f, 1f, 1f);
  private final Color icedTint = new Color(0.55f, 0.8f, 1f, 1f);
  private final Color frostStep = new Color();
  private final Color radioactiveSun = new Color(0.6f, 1f, 0.45f, 1f);
  // Reflected shots belong to the zombie now, so they must not read as one of your peas.
  private final Color reflectedPea = new Color(1f, 0.42f, 0.3f, 1f);
  /** The doc's polish list: a flash on damage, a warning tint near the house, a landing burst. */
  private final HitEffects hits = new HitEffects();
  private final Color tinted = new Color();
  private final Color hitFlash = new Color(1f, 0.94f, 0.86f, 1f);
  private final Color nearHouse = new Color(1f, 0.55f, 0.5f, 1f);
  private final Color burstColor = new Color(1f, 0.92f, 0.55f, 1f);
  // King's aura pulse and the Juggler's spin both read this; it only ticks in render().
  private float clock;
  // The match's own tick, for lining a plant's attack clip up with the shot it just fired.
  private int currentTick;
  private TextureRegion octopus;
  private boolean octopusChecked;

  public EntityRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  public LawnGeometry getGeometry() {
    return geometry;
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null) {
      return;
    }
    Board board = game.getBoard();
    currentTick = game.getCurrentTick();
    clock += delta;
    hits.advance(delta);
    observeForEffects(board);
    drawSprites(context, board, delta);
    drawShapes(context, board);
    hits.endFrame(geometry.getColumns());
    playback.endFrame();
  }

  private void drawSprites(RenderContext context, Board board, float delta) {
    context.getBatch().begin();
    TextureRegion sheep = hudArt.find("sheep");
    for (Plant plant : board.getPlants()) {
      // A Wizard's curse turns the plant into a harmless sheep until the wizard dies, so the
      // board has to show a sheep, not a plant that has quietly stopped shooting.
      boolean cursed = plant.isCursed() && sheep != null;
      context.getBatch().setColor(flashed(plantTint(plant), plant));
      if (!cursed && drawPlantAnimation(context, plant, delta)) {
        continue;
      }
      TextureRegion art = cursed ? sheep : plantArt.find(plant.getName());
      if (art == null) {
        continue;
      }
      // the fleece is one effect frame, not a seed packet, so it is sized to the tile directly
      float scale = cursed
          ? geometry.getCellHeight() * PLANT_ROW_FILL / art.getRegionHeight()
          : scaleFor(art, PLANT_REFERENCE_HEIGHT, PLANT_ROW_FILL);
      drawStanding(context, art, plant.getCol(), plant.getRow(), scale,
          PLANT_FOOT_INSET + idleBobFraction(plant));
    }
    // After the plants and in its own pass, so an octopus is never hidden under the neighbour
    // drawn next. Animated plants take the `continue` above and would otherwise be skipped.
    context.getBatch().setColor(Color.WHITE);
    for (Plant plant : board.getPlants()) {
      drawOctopusHold(context, plant);
    }
    context.getBatch().setColor(Color.WHITE);
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      drawKingAura(context, zombie);
      context.getBatch().setColor(flashed(zombieTint(zombie), zombie));
      if (drawZombieAnimation(context, zombie, delta)) {
        continue;
      }
      TextureRegion art = zombieArt.find(zombie.getName());
      if (art != null) {
        drawStanding(context, art, onBoard(zombie.getX()), zombie.getRow(),
            zombieScale(zombie, art), ZOMBIE_FOOT_INSET, spinAngle(zombie));
      }
    }
    context.getBatch().setColor(Color.WHITE);
    drawProjectiles(context, board);
    TextureRegion sun = hudArt.find("sun");
    if (sun != null) {
      for (Sun s : board.getSuns()) {
        // the doc wants the sun kinds told apart; a big one is bigger and a radioactive one glows
        context.getBatch().setColor(s.getType() == SunType.RADIOACTIVE ? radioactiveSun : Color.WHITE);
        drawCentred(context, sun, s.getX(), s.getY(),
            geometry.getCellHeight() * (s.getType() == SunType.LARGE ? 0.58f : 0.42f));
      }
      context.getBatch().setColor(Color.WHITE);
    }
    context.getBatch().end();
  }

  /** A zombie past the mower still exists in the model; keep it on the lawn instead of off-screen.
   * Only the left side is clamped, zombies spawn to the right of the lawn and walk in. */
  private double onBoard(double column) {
    return Math.max(0.0, column);
  }


  /**
   * Frozen and chilled have to read on sight, and the animation layer already multiplies whatever
   * colour the batch is set to, so a tint covers rigged and portrait entities alike.
   */
  private Color zombieTint(Zombie zombie) {
    if (zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      return frozenTint;
    }
    if (zombie.getActiveEffects().containsKey(StatusEffect.CHILLED)) {
      return chilledTint;
    }
    if (zombie.isHypnotized()) {
      return hypnoTint;
    }
    // The doc asks for a reddish warning on a zombie close to the house, which is the last thing
    // a player notices in time to do something about it.
    return zombie.getX() <= NEAR_HOUSE_COLUMN ? nearHouse : Color.WHITE;
  }

  /**
   * Hands this frame's board to the effect tracker before anything is drawn.
   *
   * <p>Separate from the drawing passes because a plant with no art is still an entity that can be
   * hit: doing this where the sprites are drawn would skip exactly those.
   */
  private void observeForEffects(Board board) {
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        hits.observe(plant, plant.getCurrentHealth());
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()) {
        hits.observe(zombie, zombie.getCurrentHealth());
      }
    }
    for (Projectile projectile : board.getProjectiles()) {
      hits.observeProjectile(projectile, projectile.getXCoordinate(),
          Math.round(projectile.getYCoordinate()));
    }
  }

  /**
   * Blends a base tint towards the flash colour.
   *
   * <p>Returns the base untouched when there is nothing to flash, so the common case still hands
   * back the shared constant rather than a fresh colour every entity every frame.
   */
  private Color flashed(Color base, Object entity) {
    float strength = hits.flashStrength(entity);
    if (strength <= 0f) {
      return base;
    }
    return tinted.set(base).lerp(hitFlash, strength);
  }

  /** The three freeze levels a plant goes through, then the solid block once it is encased. */
  private Color plantTint(Plant plant) {
    if (plant.getIceHealth() > 0) {
      return icedTint;
    }
    int level = plant.getFreezeLevel();
    if (level <= 0) {
      return Color.WHITE;
    }
    float depth = Math.min(1f, level / FREEZE_LEVELS);
    return frostStep.set(1f - 0.4f * depth, 1f - 0.18f * depth, 1f, 1f);
  }
  /**
   * The octopus a beach thrower left clamped to a plant.
   *
   * <p>A held plant simply stops acting, which on its own looks like the plant is broken rather
   * than caught, so the thing holding it has to be on the board. Sits high on the tile and slightly
   * right of centre, the way it lands in the original, and rides the plant's own idle bob so the
   * two read as one object rather than a sticker over a moving plant.
   */
  private void drawOctopusHold(RenderContext context, Plant plant) {
    if (!plant.isHeldByOctopus(currentTick)) {
      return;
    }
    TextureRegion octopus = octopusArt();
    if (octopus == null) {
      return;
    }
    float height = geometry.getCellHeight() * OCTOPUS_ROW_FILL;
    float width = octopus.getRegionWidth() * height / octopus.getRegionHeight();
    float x = geometry.columnCentreX(plant.getCol()) - width / 2f
        + geometry.getCellWidth() * OCTOPUS_NUDGE_RIGHT;
    float y = geometry.rowToY(plant.getRow())
        + geometry.getCellHeight() * (OCTOPUS_LIFT + idleBobFraction(plant));
    context.getBatch().draw(octopus, x, y, width, height);
  }

  /** Looked up once: it is one region of the thrower's rig, and the miss is worth caching too. */
  private TextureRegion octopusArt() {
    if (!octopusChecked) {
      octopusChecked = true;
      octopus = zombieArt.findPart(OCTOPUS_RIG, OCTOPUS_REGION);
    }
    return octopus;
  }

  /** Slow up/down drift, phase-shifted per tile so neighbouring plants do not bob in lockstep. */
  private float idleBobFraction(Plant plant) {
    float phase = (plant.getRow() * 3 + plant.getCol()) * 0.9f;
    return PLANT_IDLE_BOB_FRACTION * (float) Math.sin(clock * PLANT_IDLE_SPEED + phase);
  }

  /**
   * Draws the plant's own cycle, or false if it has no rig and the portrait has to do.
   *
   * <p>Idle is all the doc asks of a plant, but a rig that has an attack clip is worth playing
   * when the plant has just acted: the model already records {@code lastActionTick}, so the swing
   * or the shot lines up with the projectile leaving instead of the plant bobbing through it.
   */
  private boolean drawPlantAnimation(RenderContext context, Plant plant, float delta) {
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plant.getName());
    String clip = animation == null ? null : plantClip(animation, plant);
    if (clip == null) {
      return false;
    }
    animation.draw(context.getBatch(), clip, playback.advance(plant, clip, delta),
        geometry.columnCentreX(plant.getCol()),
        geometry.rowToY(plant.getRow()) + geometry.getCellHeight() * PLANT_FOOT_INSET,
        scaleFor(animation.width(clip), PLANT_ANIM_UNITS, PLANT_ROW_FILL), false);
    return true;
  }

  /**
   * Attack for a moment after the plant acts, idle otherwise.
   *
   * <p>"idle" is asked for before "attack" so a plant that has both rests between shots, and
   * "attack" is the fallback for the handful of rigs with no idle at all: Grave Buster is authored
   * as attack / attack1 / water because chewing a grave is the only thing it ever does, and asking
   * only for "idle" left it silently falling back to its seed packet.
   */
  private String plantClip(EntityAnimation animation, Plant plant) {
    if (justActed(plant)) {
      String attack = animation.pickClip("attack");
      if (attack != null) {
        return attack;
      }
    }
    return animation.pickClip("idle", "attack");
  }

  /** True for {@link #PLANT_ATTACK_HOLD} seconds after the plant's last action tick. */
  private boolean justActed(Plant plant) {
    int sinceAction = currentTick - plant.getLastActionTick();
    return plant.getLastActionTick() > 0 && sinceAction >= 0
        && sinceAction < PLANT_ATTACK_HOLD_TICKS;
  }

  /** Same for a zombie, which unlike a plant has to switch clips as it goes. */
  private boolean drawZombieAnimation(RenderContext context, Zombie zombie, float delta) {
    EntityAnimation animation = zombieAnimation(zombie);
    if (animation == null) {
      return false;
    }
    String clip = zombieClip(animation, zombie);
    animation.draw(context.getBatch(), clip,
        playback.advance(zombie, clip, delta * animationRate(zombie)),
        geometry.columnCentreX(onBoard(zombie.getX())),
        geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET,
        zombieAnimationScale(zombie, animation, clip), zombie.isHypnotized(),
        armourVisibility(animation, zombie));
    return true;
  }

  /**
   * Which armour pieces of the rig to show.
   *
   * <p>The walker rigs carry their armour with them: one body, plus a cone, a bucket, a brick, a
   * crown and shoulder plates parked on the same skeleton and hidden by default. That is why the
   * cone-head and the plain zombie are one sheet in Zombies.json's art and four rows in its data.
   * Showing the pieces this zombie is actually wearing is what keeps the four apart on the lawn,
   * and dropping a piece the moment its {@link Armor} is destroyed is the doc's "armour is visible
   * while it has health and gone once it does not".
   *
   * <p>Returns null -- the rig's authored default, everything optional hidden -- for a zombie
   * wearing nothing, which is every zombie whose armour is drawn into its own body art.
   */
  private static Map<String, Boolean> armourVisibility(EntityAnimation animation, Zombie zombie) {
    List<Armor> armors = zombie.getArmors();
    if (armors.isEmpty()) {
      return null;
    }
    Map<String, Boolean> visibility = new HashMap<>();
    boolean any = false;
    for (String part : animation.partNames()) {
      String lower = part.toLowerCase();
      String group = ArmourParts.groupOf(lower);
      if (group == null) {
        continue;
      }
      Armor worn = wornArmour(armors, group);
      // A damage stage only shows at its own wear level, so a bucket dents as it is shot off
      boolean show = worn != null && stageMatches(lower, worn);
      visibility.put(part, show);
      any |= show;
    }
    return any ? visibility : null;
  }

  /** The still-intact armour this zombie wears in that group, or null. */
  private static Armor wornArmour(List<Armor> armors, String group) {
    for (Armor armor : armors) {
      if (armor.isDestroyed() || armor.getType() == null) {
        continue;
      }
      if (group.equals(switch (armor.getType()) {
        case CONE -> ArmourParts.CONE;
        case BUCKET -> ArmourParts.BUCKET;
        case BLOCK -> ArmourParts.BRICK;
        case HELMET -> ArmourParts.CROWN;
        case SHOULDER_ARMOR -> ArmourParts.SHOULDER;
        // newspaper, barrel and piano are drawn into their zombie's own body art
        default -> "";
      })) {
        return armor;
      }
    }
    return null;
  }

  /**
   * Each armour is authored in three states -- whole, {@code damage_01}, {@code damage_02} -- so
   * the one on show is chosen by how much of it is left, and only that one.
   */
  private static boolean stageMatches(String lowerPartName, Armor armor) {
    float left = armor.getCurrentHealth() / (float) Math.max(1, armor.getMaxHealth());
    boolean isStage1 = lowerPartName.contains("damage_01") || lowerPartName.endsWith("damaged1");
    boolean isStage2 = lowerPartName.contains("damage_02") || lowerPartName.endsWith("damaged2");
    if (!isStage1 && !isStage2) {
      // the whole piece, plus the wrapper parts that hold the states together
      return lowerPartName.contains("states") || left > ARMOUR_STAGE_1;
    }
    return isStage1 ? left <= ARMOUR_STAGE_1 && left > ARMOUR_STAGE_2 : left <= ARMOUR_STAGE_2;
  }

  /** Null unless this zombie has a rig with a clip worth playing. */
  private EntityAnimation zombieAnimation(Zombie zombie) {
    EntityAnimation animation = animations.find(AnimationLibrary.ZOMBIES, zombie.getName());
    return animation != null && zombieClip(animation, zombie) != null ? animation : null;
  }

  /** Eating while it chews a plant, walking the rest of the time. */
  private static String zombieClip(EntityAnimation animation, Zombie zombie) {
    if (zombie.isEating()) {
      String eat = animation.pickClip("eat");
      if (eat != null) {
        return eat;
      }
    }
    // "play" is the Pianist: his rig has no walk cycle because the piano-playing loop is how he
    // travels, so asking only for walk left him standing still while he crossed the lawn.
    return animation.pickClip("walk", "play", "idle");
  }

  /** Frozen holds the pose and chilled halves it, matching what Zombie.move() actually does. */
  private static float animationRate(Zombie zombie) {
    if (zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      return 0f;
    }
    return zombie.getActiveEffects().containsKey(StatusEffect.CHILLED) ? CHILLED_ANIM_RATE : 1f;
  }

  private float zombieAnimationScale(Zombie zombie, EntityAnimation animation, String clip) {
    float height = animation.height(clip);
    if (isBoss(zombie)) {
      return height > 0f ? geometry.getCellHeight() * ZOMBOSS_ROW_FILL / height : 0f;
    }
    return scaleFor(animation.width(clip), ZOMBIE_ANIM_UNITS, ZOMBIE_ROW_FILL);
  }

  /** How tall the zombie is drawn, whichever way it is being drawn. */
  private float zombieSpriteHeight(Zombie zombie) {
    EntityAnimation animation = zombieAnimation(zombie);
    if (animation != null) {
      String clip = zombieClip(animation, zombie);
      return animation.height(clip) * zombieAnimationScale(zombie, animation, clip);
    }
    TextureRegion art = zombieArt.find(zombie.getName());
    return art == null
        ? geometry.getCellHeight() * 0.6f
        : art.getRegionHeight() * zombieScale(zombie, art);
  }

  private static boolean isBoss(Zombie zombie) {
    return zombie.getBehavior() instanceof ZombossAction;
  }

  /** Walkers share one reference height so a gargantuar stays bigger; Zomboss is its own size. */
  private float zombieScale(Zombie zombie, TextureRegion art) {
    return isBoss(zombie)
        ? geometry.getCellHeight() * ZOMBOSS_ROW_FILL / art.getRegionHeight()
        : scaleFor(art, ZOMBIE_REFERENCE_HEIGHT, ZOMBIE_ROW_FILL);
  }

  private float scaleFor(TextureRegion region, float referenceHeight, float rowFill) {
    return scaleFor(region.getRegionWidth(), referenceHeight, rowFill);
  }

  private float scaleFor(float spriteWidth, float referenceHeight, float rowFill) {
    float scale = geometry.getCellHeight() * rowFill / referenceHeight;
    // nothing may spill sideways into the neighbouring lane
    float widest = geometry.getCellWidth() * 0.95f;
    if (spriteWidth * scale > widest) {
      scale = widest / spriteWidth;
    }
    return scale;
  }

  /** Feet on the ground rather than centred, so plants look planted and zombies look upright. */
  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset) {
    drawStanding(context, region, col, row, scale, footInset, 0f);
  }

  /** Same, but spun about its own middle. Only the juggler uses a non-zero angle. */
  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset, float angle) {
    float width = region.getRegionWidth() * scale;
    float height = region.getRegionHeight() * scale;
    float bottom = geometry.rowToY(row) + geometry.getCellHeight() * footInset;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f, bottom,
        width / 2f, height / 2f, width, height, 1f, 1f, angle);
  }

  /**
   * The juggler spins while shots keep coming at it and moves faster the whole time, and every
   * shot it catches goes back at the plants. Standing still while that happens would leave the
   * player with no idea why their peas turned around, so it actually spins on screen.
   */
  private float spinAngle(Zombie zombie) {
    return zombie.getBehavior() instanceof JesterZombieAction jester && jester.isSpinning()
        ? (clock * 540f) % 360f
        : 0f;
  }

  /**
   * The King never moves and never eats; what he does is speed up everything in his lane. That
   * is invisible unless the reach is drawn, so his own knighting burst marks it out.
   */
  private void drawKingAura(RenderContext context, Zombie zombie) {
    TextureRegion aura = hudArt.find("kingaura");
    if (aura == null || !(zombie.getBehavior() instanceof KingAuraZombieAction)) {
      return;
    }
    float size = geometry.getCellWidth() * 2.6f;
    float pulse = 0.32f + 0.12f * (float) Math.sin(clock * 2.5f);
    context.getBatch().setColor(1f, 0.9f, 0.45f, pulse);
    context.getBatch().draw(aura, geometry.columnCentreX(onBoard(zombie.getX())) - size / 2f,
        geometry.rowCentreY(zombie.getRow()) - size / 2f, size, size);
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawProjectiles(RenderContext context, Board board) {
    for (Projectile projectile : board.getProjectiles()) {
      ProjectileArt.Shot shot = projectileArt.find(projectile);
      if (shot == null) {
        continue;
      }
      // the juggler throws your own shots back at you; those have to look wrong
      context.getBatch().setColor(projectile.isFromZombie() ? reflectedPea : Color.WHITE);
      drawCentred(context, shot.region(), projectile.getXCoordinate(),
          projectile.getYCoordinate(), geometry.getCellHeight() * shot.rowFraction(),
          arcLift(projectile), shot.angle());
    }
    context.getBatch().setColor(Color.WHITE);
  }

  /**
   * How high above its lane an arcing shot is right now: nothing as it leaves the plant, a full
   * arc in the middle, back down where it was aimed. Straight shots stay on the lane.
   */
  private float arcLift(Projectile projectile) {
    if (!projectile.isLobbed()) {
      return 0f;
    }
    double span = projectile.getTargetX() - projectile.getLaunchX();
    if (span <= 0) {
      return 0f;
    }
    double travelled = (projectile.getXCoordinate() - projectile.getLaunchX()) / span;
    if (travelled <= 0 || travelled >= 1) {
      return 0f;
    }
    return (float) (4 * travelled * (1 - travelled))
        * geometry.getCellHeight() * LOB_ARC_HEIGHT;
  }

  private void drawCentred(RenderContext context, TextureRegion region, double col, double row,
      float targetHeight) {
    drawCentred(context, region, col, row, targetHeight, 0f, 0f);
  }

  private void drawCentred(RenderContext context, TextureRegion region, double col, double row,
      float targetHeight, float lift, float angle) {
    float scale = targetHeight / region.getRegionHeight();
    float width = region.getRegionWidth() * scale;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f,
        geometry.rowCentreY((int) Math.round(row)) - targetHeight / 2f + lift,
        width / 2f, targetHeight / 2f, width, targetHeight, 1f, 1f, angle);
  }

  private void drawShapes(RenderContext context, Board board) {
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);

    shapes.setColor(peaColor);
    for (Projectile projectile : board.getProjectiles()) {
      if (projectileArt.find(projectile) == null) {
        shapes.circle(geometry.columnCentreX(projectile.getXCoordinate()),
            geometry.rowCentreY(Math.round(projectile.getYCoordinate())), 7f);
      }
    }
    if (hudArt.find("sun") == null) {
      shapes.setColor(sunColor);
      for (Sun sun : board.getSuns()) {
        shapes.circle(geometry.columnCentreX(sun.getX()),
            geometry.rowCentreY((int) Math.round(sun.getY())), 16f);
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()) {
        healthBar(shapes, zombie);
      }
    }
    drawHitBursts(shapes);
    shapes.end();

    // A zombie with no verified portrait gets an outline rather than someone elses art.
    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(noArt);
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombieArt.find(zombie.getName()) == null
          && zombieAnimation(zombie) == null) {
        shapes.rect(geometry.columnCentreX(onBoard(zombie.getX())) - geometry.getCellWidth() * 0.25f,
            geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * 0.1f,
            geometry.getCellWidth() * 0.5f, geometry.getCellHeight() * 0.7f);
      }
    }
    // The same for a plant. Three of them -- Cat-tail, Pierce-mint and catTail-mint -- have
    // neither a rig nor a seed packet anywhere in the asset library, and drawing nothing at all
    // meant planting one left an apparently empty tile that still took a seed and still shot.
    for (Plant plant : board.getPlants()) {
      if (plant.isDead() || hasPlantArt(plant)) {
        continue;
      }
      shapes.rect(geometry.columnCentreX(plant.getCol()) - geometry.getCellWidth() * 0.22f,
          geometry.rowToY(plant.getRow()) + geometry.getCellHeight() * 0.16f,
          geometry.getCellWidth() * 0.44f, geometry.getCellHeight() * 0.56f);
    }
    shapes.end();
  }

  /**
   * The small burst where a projectile landed.
   *
   * <p>Shapes rather than art: assets/metadata/asset-map.json resolved no effect art at all, so
   * there is no impact sprite in the library to use, and a ring that grows and fades reads as a
   * hit without pretending to be something it is not.
   */
  private void drawHitBursts(ShapeRenderer shapes) {
    for (HitEffects.Burst burst : hits.getBursts()) {
      burstColor.a = burst.alpha() * 0.75f;
      shapes.setColor(burstColor);
      shapes.circle(geometry.columnCentreX(burst.column()),
          geometry.rowCentreY(burst.row()),
          geometry.getCellHeight() * burst.radiusFraction());
    }
    burstColor.a = 1f;
  }

  /** Whether anything at all is drawn for this plant: its own rig, or failing that its packet. */
  private boolean hasPlantArt(Plant plant) {
    if (plant.isCursed() && hudArt.find("sheep") != null) {
      return true;
    }
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plant.getName());
    return (animation != null && plantClip(animation, plant) != null)
        || plantArt.find(plant.getName()) != null;
  }

  /** Sits just above the sprite, so a tall zombie does not wear its bar on its chest. */
  private void healthBar(ShapeRenderer shapes, Zombie zombie) {
    float spriteHeight = zombieSpriteHeight(zombie);
    // the chapter ends when this one dies, so its bar has to be readable from across the lawn
    float width = geometry.getCellWidth() * (isBoss(zombie) ? 1.9f : 0.55f);
    float x = geometry.columnCentreX(onBoard(zombie.getX())) - width / 2f;
    float y = geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + spriteHeight + 4f;
    // a tall zombie in the top lane would otherwise wear its bar up in the seed cards
    y = Math.min(y, geometry.rowToY(0) + geometry.getCellHeight() - 7f);
    float fraction = zombie.getCurrentHealth() / (float) Math.max(1, zombie.getMaxHealth());
    fraction = Math.max(0f, Math.min(1f, fraction));
    float thickness = isBoss(zombie) ? 11f : 5f;
    shapes.setColor(healthBack);
    shapes.rect(x - 1f, y - 1f, width + 2f, thickness + 2f);
    shapes.setColor(fraction < 0.35f ? healthLow : healthFront);
    shapes.rect(x, y, width * fraction, thickness);
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
    projectileArt.dispose();
    animations.dispose();
  }
}
