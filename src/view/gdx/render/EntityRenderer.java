package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.core.GameManager;
import model.enums.StatusEffect;
import model.enums.SunType;
import model.game.Board;
import model.game.BossHazard;
import model.game.LootDropper;
import model.game.PlantFoodDrop;
import model.game.Projectile;
import model.game.Sun;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.behavior.BarrelRollerZombieAction;
import model.game.zombie.behavior.EnrageOnArmorBreakZombieAction;
import model.game.zombie.behavior.GargantuarAction;
import model.game.zombie.behavior.HookPullZombieAction;
import model.game.zombie.behavior.HunterZombieAction;
import model.game.zombie.behavior.JesterZombieAction;
import model.game.zombie.behavior.KingAuraZombieAction;
import model.game.zombie.behavior.OctopusThrowerZombieAction;
import model.game.zombie.behavior.PianistZombieAction;
import model.game.zombie.behavior.RaHealAuraZombieAction;
import model.game.zombie.behavior.TacklerZombieAction;
import model.game.zombie.behavior.TombRaiserZombieAction;
import model.game.zombie.behavior.TurquoiseZombieAction;
import model.game.zombie.behavior.WizardZombieAction;
import model.game.zombie.behavior.ZombossAction;
import view.gdx.animation.AnimationLibrary;
import view.gdx.animation.AnimationStates;
import view.gdx.animation.EntityAnimation;
import view.gdx.core.GdxConfig;
import view.gdx.ui.CurrencyArt;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.ProjectileArt;
import view.gdx.ui.ZombieArt;

public final class EntityRenderer implements WorldRenderer {

  private static final float ZOMBIE_REFERENCE_HEIGHT = 104f;
  private static final float ZOMBIE_ROW_FILL = 0.92f;
  private static final float PLANT_REFERENCE_HEIGHT = 70f;
  private static final float PLANT_ROW_FILL = 0.78f;
  private static final float ZOMBOSS_ROW_FILL = 2.4f;
  private static final float ZOMBIE_FOOT_INSET = 0.08f;

  private static final float ZOMBIE_WIDTH_LIMIT_LANES = 1.62f;
  private static final float ZOMBIE_HEIGHT_LIMIT_LANES = 1.7f;
  private static final float PLANT_WIDTH_LIMIT_LANES = 1.06f;
  private static final float ZOMBOTANY_REFERENCE_HEIGHT = 62f;

  private static final double NEAR_HOUSE_COLUMN = 1.2;
  private static final float PLANT_FOOT_INSET = 0.14f;

  private static final String SHADOW_REGION = "potshadow";
  private static final float SHADOW_ALPHA = 0.36f;
  private static final float PLANT_SHADOW_WIDTH_LANES = 0.74f;
  private static final float ZOMBIE_SHADOW_WIDTH_LANES = 0.66f;
  private static final float PLANT_IDLE_SPEED = 2.2f;
  private static final float PLANT_IDLE_BOB_FRACTION = 0.02f;
  private static final float PLANT_ANIM_UNITS = 90f;
  private static final float ZOMBIE_ANIM_UNITS = 150f;
  private static final String ZOMBOTANY_BODY = "ZombieTutorialDefault";
  private static final float ZOMBOTANY_HEAD_FILL = 1.6f;
  private static final float THROW_ARC_HEIGHT = 0.9f;
  private static final float CHILLED_ANIM_RATE = 0.5f;
  private static final float ARMOUR_STAGE_1 = 0.66f;
  private static final float ARMOUR_STAGE_2 = 0.33f;
  private static final int PLANT_ATTACK_HOLD_TICKS = 4;
  private static final float LOB_ARC_HEIGHT = 0.85f;
  /** How far across the plant's head the mouth sits, as a share of the head's own width. */
  private static final float MUZZLE_HEAD_REACH = 0.35f;
  /**
   * Where in the firing motion the shot actually leaves, as a share of the attack clip.
   *
   * <p>One means the very end of it: the pea is held in the mouth for the whole of the shooting
   * animation and only sets off as the plant finishes the throw. It used to be 0.4, so the shot
   * left a third of the way in, while the plant was still winding up, and the release read as
   * unrelated to the animation playing.
   */
  private static final float MUZZLE_RELEASE_OF_CLIP = 1f;
  private static final float MUZZLE_MIN_RELEASE = 0.35f;
  /**
   * Never a whole tick. The shot has already done its damage on the tick it was fired, so holding
   * it in the mouth for all of that tick would leave the hit landing on a pea that had not left
   * yet; this keeps the release inside the tick it belongs to.
   */
  private static final float MUZZLE_MAX_RELEASE = 0.92f;
  private static final float MUZZLE_MAX_FORWARD = 0.5f;
  private static final float MUZZLE_MAX_LIFT_LANES = 0.4f;
  private static final float FREEZE_LEVELS = Plant.MAX_FREEZE_LEVEL;
  /** How much white a full-strength hit adds on top of the sprite. */
  private static final float HIT_FLASH_LIFT = 0.4f;

  private static final String PLANT_FOOD_GLOW_REGION = "whiteburst";
  private static final float PLANT_FOOD_GLOW_LANES = 1.5f;
  private static final float PLANT_FOOD_GLOW_SPEED = 7f;
  /** The plant food itself, lying on the lawn waiting to be picked up. */
  private static final String PLANT_FOOD_REGION = "plantfood";
  private static final float PLANT_FOOD_DROP_LANES = 0.5f;
  private static final float PLANT_FOOD_BOB_LANES = 0.06f;
  /** Ticks of fading left before a dose disappears. */
  private static final int PLANT_FOOD_FADE_TICKS = 25;

  /**
   * The ice a frozen zombie sits in, borrowed from the Iceberg Lettuce's own rig: it is the frozen
   * mass that plant is drawn inside, and the only block of ice the game ships.
   */
  private static final String ICE_BLOCK_RIG = "iceberglettuce";
  private static final String ICE_BLOCK_REGION = "iceburg_85x80";
  /** The pat of butter Kernel-pult drops, which is what a stunned zombie is wearing. */
  private static final String BUTTER_RIG = "kernelpult";
  private static final String BUTTER_REGION = "kernalpult_34x37";
  private static final float BUTTER_FILL = 0.30f;
  private static final float BUTTER_WOBBLE = 0.03f;
  private static final float ICE_RIM_FILL = 1.06f;
  private static final float ICE_BLOCK_FILL = 1.18f;
  private static final float ICE_BLOCK_SINK = 0.08f;

  /** A plant sits in its tile rather than standing over it, so its block is a lane-sized one. */
  private static final float PLANT_ICE_BLOCK_FILL = 1.06f;

  private static final String OCTOPUS_RIG = "zombiebeachoctopus";
  private static final String OCTOPUS_REGION = "zombie_beach_octopus_66x76";
  private static final float OCTOPUS_ROW_FILL = 0.44f;

  private static final String SPLAT_REGION = "splatpea";
  private static final String PEA_REGION = "pea";
  private static final String SNOW_REGION = "snowgust";
  private static final String DUST_REGION = "dustpuff";
  /**
   * The flash of an explosion, tinted warm for a fire blast and pale for a concussive one.
   *
   * <p>The hud's burst art, which is a filled yellow-white star. Not "whiteburst", the other
   * candidate, whose middle is transparent -- blown up to the size of a Cherry Bomb's blast that
   * reads as a hole in the lawn rather than as a bang.
   */
  private static final String BLAST_REGION = "armourbreak";
  private static final String ASH_REGION = "zombieash";
  private static final String DIRT_REGION = "dirtclods";
  private static final String PLANT_PUFF_REGION = "plantpuff";
  private static final String ARMOUR_BREAK_REGION = "armourbreak";

  /** The rig the little sharks are cut from: they are the beach boss's own mech jaws. */
  private static final String SHARK_RIG = "zombiezombossmechpirate";
  private static final String SHARK_PART = "Zomboss_shark_jaw";
  private static final float HAZARD_SIZE_LANES = 0.44f;
  private static final float SHARK_SIZE_LANES = 0.5f;

  /** Where each piece leaves the body and how big it is drawn, both as a share of a lane. */
  private static final float HEAD_GIB_LIFT = 0.62f;
  private static final float HEAD_GIB_SIZE = 0.30f;
  private static final float ARM_GIB_LIFT = 0.45f;
  private static final float ARM_GIB_SIZE = 0.26f;
  private static final float ARMOUR_GIB_LIFT = 0.72f;
  private static final float ARMOUR_GIB_SIZE = 0.34f;
  static final String SPARK_ARMOUR = "armour";
  static final String SPARK_PLANTED = "planted";
  static final String SPARK_SUN = "sun";
  private static final float SPLAT_SIZE_LANES = 0.42f;
  private static final float DUST_SIZE_LANES = 0.95f;
  private static final float ASH_SIZE_LANES = 0.5f;
  private static final float SPARK_SIZE_LANES = 0.8f;

  /** How big the plant-food marker over a glowing zombie is, as a fraction of a lane. */
  private static final float PLANT_FOOD_MARK_FRACTION = 0.3f;
  private static final float LOOT_ICON_FRACTION = 0.34f;
  private static final float LOOT_LIFT_FRACTION = 0.7f;

  private static final float SHAKE_PER_DEATH = 3.4f;
  private static final float MAX_SHAKE = 9f;
  private static final float SHAKE_DECAY_PER_SECOND = 22f;
  private static final float OCTOPUS_LIFT = 0.52f;
  private static final float OCTOPUS_NUDGE_RIGHT = 0.10f;

  private final LawnGeometry geometry;
  private final PlantArt plantArt = new PlantArt();
  private final ZombieArt zombieArt = new ZombieArt();
  private final HudArt hudArt = new HudArt();
  private final ProjectileArt projectileArt = new ProjectileArt();
  private final CurrencyArt currencyArt = new CurrencyArt();
  private final AnimationLibrary animations = new AnimationLibrary();
  private final AnimationStates playback = new AnimationStates();
  private final Color healthBack = new Color(0f, 0f, 0f, 0.55f);
  private final Color healthFront = new Color(0.25f, 0.85f, 0.3f, 0.95f);
  private final Color healthLow = new Color(0.9f, 0.5f, 0.15f, 0.95f);
  /** The armour strip: a cold grey-blue, so it never reads as more health. */
  private final Color armourTint = new Color(0.72f, 0.78f, 0.88f, 0.95f);
  /** A rocket is not a pea: dark and metallic, so the two are never confused mid-flight. */
  private final Color missileTint = new Color(0.42f, 0.40f, 0.44f, 1f);
  private final Color hazardTarget = new Color(0.95f, 0.35f, 0.25f, 1f);
  private final Color peaColor = new Color(0.55f, 0.9f, 0.3f, 1f);
  private final Color sunColor = new Color(1f, 0.85f, 0.2f, 1f);
  private final Color noArt = new Color(1f, 1f, 1f, 0.85f);
  private final Color frozenTint = new Color(0.45f, 0.7f, 1f, 1f);
  private final Color chilledTint = new Color(0.72f, 0.88f, 1f, 1f);
  private final Color hypnoTint = new Color(0.85f, 0.6f, 1f, 1f);
  private final Color poisonedTint = new Color(0.62f, 1f, 0.45f, 1f);
  private final Color shieldTint = new Color(0.6f, 0.8f, 0.98f, 1f);
  private final Color icedTint = new Color(0.55f, 0.8f, 1f, 1f);
  /** Pale blue and see-through, so the zombie still reads through the ice it is stuck in. */
  private final Color iceBlockTint = new Color(0.55f, 0.83f, 1f, 0.82f);
  private final Color iceRimTint = new Color(0.5f, 0.8f, 1f, 0.7f);
  /** The green a plant-food dose glows, and the colour of the food itself lying on the lawn. */
  private final Color plantFoodGlow = new Color(0.45f, 1f, 0.35f, 1f);
  private final Color frostStep = new Color();
  /** Purple, which is the colour the doc names for a radioactive sun; it used to be green. */
  private final Color radioactiveSun = new Color(0.78f, 0.42f, 1f, 1f);
  // Reflected shots belong to the zombie now, so they must not read as one of your peas.
  private final Color reflectedPea = new Color(1f, 0.42f, 0.3f, 1f);
  /** The doc's polish list: a flash on damage, a warning tint near the house, a landing burst. */
  private final HitEffects hits = new HitEffects();
  private final Color tinted = new Color();
  private final Color hitFlash = new Color(1f, 0.94f, 0.86f, 1f);
  private final Color nearHouse = new Color(1f, 0.55f, 0.5f, 1f);
  private final Color burstColor = new Color(1f, 0.92f, 0.55f, 1f);
  private final Color dustColor = new Color(0.4f, 0.35f, 0.28f, 1f);
  private float clock;
  private float shakeMagnitude;
  private float shakeSeed;
  private int currentTick;
  /** How far this frame sits between two simulation ticks; everything moving is drawn along it. */
  private float tickAlpha;
  private TextureRegion octopus;
  private boolean octopusChecked;
  private TextureRegion iceBlockRegion;
  private boolean butterChecked;
  private TextureRegion butterRegion;
  private boolean iceBlockChecked;
  private final Map<Plant, Boolean> knownPlants = new java.util.IdentityHashMap<>();
  private final Map<Plant, Boolean> seenPlants = new java.util.IdentityHashMap<>();
  /** The tick each plant was first seen on, for the clips that play once when one goes down. */
  private final Map<Plant, Integer> plantedAt = new java.util.IdentityHashMap<>();
  /**
   * Zombies that died this frame, kept only by the view so their death clip can finish.
   *
   * <p>The board drops a dead zombie inside the same tick it dies -- Board.cleanupEntities runs
   * removeIf(Zombie::isDead) before the frame is ever drawn -- so by the time anything renders,
   * the zombie is already gone from getZombies(). Nothing that reads the board can therefore show
   * a death: the corpse has to be caught as it disappears and held here for the length of its own
   * die clip. Same reason HitEffects watches projectiles by disappearance rather than by state.
   */
  private final List<DyingZombie> dying = new java.util.ArrayList<>();
  /** Heads, arms and armour in the air. See {@link DetachedParts}. */
  private final DetachedParts debris = new DetachedParts();
  /** Armour each zombie was still wearing last frame, so a piece can be thrown when one goes. */
  private final Map<Zombie, Set<String>> wornArmourGroups = new java.util.IdentityHashMap<>();
  private final Map<Zombie, Boolean> zombiesLastFrame = new java.util.IdentityHashMap<>();
  private final Map<Zombie, Boolean> zombiesThisFrame = new java.util.IdentityHashMap<>();
  private final Map<Sun, double[]> knownSuns = new java.util.IdentityHashMap<>();
  private final Map<Sun, double[]> seenSuns = new java.util.IdentityHashMap<>();
  /** Muzzle of each plant drawn this frame, filled in as the lawn is drawn. See muzzleOf. */
  private final Map<Plant, float[]> muzzles = new java.util.IdentityHashMap<>();
  /** The same for the zombies that shoot: a Zombotany pea, a Hunter's ice. */
  private final Map<Zombie, float[]> zombieMuzzles = new java.util.IdentityHashMap<>();
  private boolean seenABoard;

  /**
   * One zombie mid-collapse: the entity itself, where it fell, and how long it has been falling.
   *
   * <p>Holds the Zombie rather than a copy of its numbers because the rig, the armour it was still
   * wearing and its facing all decide how the death is drawn, and the object stays perfectly
   * readable after the board lets go of it.
   */
  private static final class DyingZombie {
    private final Zombie zombie;
    private final double column;
    private final int row;
    private float age;
    /** Set when the gib was thrown, so the corpse is drawn without the piece that came off. */
    private boolean headOff;
    private boolean armOff;

    private DyingZombie(Zombie zombie, double column, int row) {
      this.zombie = zombie;
      this.column = column;
      this.row = row;
    }
  }

  public EntityRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  public LawnGeometry getGeometry() {
    return geometry;
  }

  /**
   * Projectiles that landed since the last call, so the screen can play the impact sound.
   *
   * <p>The impact is worked out here rather than in the model -- {@link HitEffects} already spots
   * a projectile that was on the lawn last frame and is not on it now, which is the same event the
   * splat is drawn for, so the sound and the visual can never disagree.
   */
  public int drainImpactCount() {
    return hits.drainFreshImpacts();
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null) {
      return;
    }
    Board board = game.getBoard();
    currentTick = game.getCurrentTick();
    tickAlpha = context.getTickAlpha();
    clock += delta;
    hits.advance(delta);
    debris.advance(delta);
    spawnLootPickups(board);
    observeForEffects(board);
    // After observeForEffects, which is what notices the fallen in the first place, and before the
    // shake reads the death count so a death still shakes the frame it happened on.
    advanceTheFallen(delta);
    applyShake(context, delta, hits.drainFreshDeaths());
    drawSprites(context, board, delta);
    drawShapes(context, board);
    hits.endFrame(geometry.getColumns());
    playback.endFrame();
  }

  private void drawGroundShadows(RenderContext context, Board board) {
    TextureRegion shadow = hudArt.find(SHADOW_REGION);
    if (shadow == null) {
      return;
    }
    context.getBatch().setColor(1f, 1f, 1f, SHADOW_ALPHA);
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        drawShadow(context, shadow, plant.getCol(), plant.getRow(),
            PLANT_SHADOW_WIDTH_LANES, PLANT_FOOT_INSET);
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()) {
        drawShadow(context, shadow, drawColumn(zombie), footRow(zombie),
            ZOMBIE_SHADOW_WIDTH_LANES, ZOMBIE_FOOT_INSET);
      }
    }
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawPlant(RenderContext context, Plant plant, TextureRegion sheep, float delta,
      boolean onWater) {
    boolean cursed = plant.isCursed() && sheep != null;
    drawPlantFoodGlow(context, plant);
    context.getBatch().setColor(flashed(plantTint(plant), plant));
    if (!cursed && drawPlantAnimation(context, plant, delta, onWater)) {
      return;
    }
    TextureRegion art = cursed ? sheep : plantArt.find(plant.getName());
    if (art == null) {
      return;
    }
    float scale = cursed
        ? geometry.getCellHeight() * PLANT_ROW_FILL / art.getRegionHeight()
        : scaleFor(art, PLANT_REFERENCE_HEIGHT, PLANT_ROW_FILL);
    drawStanding(context, art, plant.getCol(), plant.getRow(), scale,
        PLANT_FOOT_INSET + idleBobFraction(plant));
  }

  private void drawZombie(RenderContext context, Zombie zombie, float delta) {
    drawKingAura(context, zombie);
    drawHealAura(context, zombie);
    Color tint = flashed(zombieTint(zombie), zombie);
    context.getBatch().setColor(tint.r, tint.g, tint.b, tint.a * zombieAlpha(zombie));
    boolean rigged = drawZombieAnimation(context, zombie, delta);
    if (!rigged) {
      TextureRegion art = zombieArt.find(zombie.getName());
      if (art != null) {
        drawStanding(context, art, drawColumn(zombie), footRow(zombie),
            zombieScale(zombie, art), ZOMBIE_FOOT_INSET, spinAngle(zombie));
      }
    }
    drawHitFlash(context, zombie, rigged);
    drawIceBlock(context, zombie);
    drawStunButter(context, zombie);
    // Last, so the carried icon sits above both the body and the ice it may be stuck in.
    drawPlantFoodCarrier(context, zombie);
  }

  /**
   * The white kick a zombie takes when it is hit.
   *
   * <p>A batch colour multiplies, so tinting alone can only ever make a sprite darker -- which is
   * why the flash was invisible on everything, the Octopus included. This lays the same frame down
   * a second time with additive blending instead, so the sprite really does brighten, and fades
   * out on its own as {@link HitEffects} lets the flash decay.
   *
   * @param rigged whether the zombie drew from its rig, so the second pass matches the first
   */
  private void drawHitFlash(RenderContext context, Zombie zombie, boolean rigged) {
    float strength = hits.flashStrength(zombie);
    if (strength <= 0f) {
      return;
    }
    float lift = strength * HIT_FLASH_LIFT;
    Batch batch = context.getBatch();
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    batch.setColor(lift, lift, lift, 1f);
    if (rigged) {
      // Zero delta: the same frame the zombie was just drawn on, not the next one.
      drawZombieAnimation(context, zombie, 0f);
    } else {
      TextureRegion art = zombieArt.find(zombie.getName());
      if (art != null) {
        drawStanding(context, art, drawColumn(zombie), footRow(zombie),
            zombieScale(zombie, art), ZOMBIE_FOOT_INSET, spinAngle(zombie));
      }
    }
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    batch.setColor(Color.WHITE);
  }

  /**
   * The block of ice a frozen zombie is stuck in. It is on screen for exactly as long as the
   * {@link StatusEffect#FROZEN} effect lasts, because that is the only thing it is asking about.
   */
  private void drawIceBlock(RenderContext context, Zombie zombie) {
    if (!zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      return;
    }
    loadIceBlock();
    if (iceBlockRegion == null) {
      return;
    }
    float height = Math.max(zombieSpriteHeight(zombie), geometry.getCellHeight() * 0.6f)
        * ICE_BLOCK_FILL;
    float centreX = geometry.columnCentreX(drawColumn(zombie));
    float bottom = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        - height * ICE_BLOCK_SINK;
    Batch batch = context.getBatch();

    batch.setColor(iceBlockTint);
    stamp(batch, iceBlockRegion, centreX, bottom, height);
    batch.setColor(Color.WHITE);
  }

  /**
   * The block of ice a frozen plant is held in, drawn over the plant so it reads as being inside.
   *
   * <p>The doc asks for a frozen plant to be shown inside a block, the way a frozen zombie already
   * was. Tinting it blue was all that happened, which reads as "cold" rather than as "held until
   * this ice is broken" -- and the ice is a 600-health thing the player has to shoot, so it has to
   * be on screen to be aimed at.
   */
  private void drawPlantIceBlock(RenderContext context, Plant plant) {
    if (plant.getIceHealth() <= 0) {
      return;
    }
    loadIceBlock();
    if (iceBlockRegion == null) {
      return;
    }
    float height = geometry.getCellHeight() * PLANT_ICE_BLOCK_FILL;
    Batch batch = context.getBatch();
    batch.setColor(iceBlockTint);
    stamp(batch, iceBlockRegion, geometry.columnCentreX(plant.getCol()),
        geometry.rowToY(plant.getRow()) + geometry.getCellHeight() * PLANT_FOOT_INSET
            - height * ICE_BLOCK_SINK, height);
    batch.setColor(Color.WHITE);
  }

  /** Draws a region standing on a baseline, centred, scaled to a height. */
  private static void stamp(Batch batch, TextureRegion region, float centreX, float bottom,
      float height) {
    float width = region.getRegionWidth() * height / region.getRegionHeight();
    batch.draw(region, centreX - width / 2f, bottom, width, height);
  }

  /**
   * The butter on a stunned zombie's head.
   *
   * <p>A stun is not a freeze. Kernel-pult's butter used to apply the FROZEN effect, so a buttered
   * zombie was drawn inside a block of ice and tinted blue -- the game telling the player it had
   * done something it had not, and hiding the one plant whose whole job is the stun. The effect is
   * its own now, and this is what it looks like: the pat of butter off Kernel-pult's own rig,
   * sitting on the zombie's head and wobbling, with no ice anywhere.
   */
  private void drawStunButter(RenderContext context, Zombie zombie) {
    if (!zombie.isStunned()) {
      return;
    }
    if (!butterChecked) {
      butterChecked = true;
      butterRegion = plantArt.findPart(BUTTER_RIG, BUTTER_REGION);
    }
    if (butterRegion == null) {
      return;
    }
    float height = geometry.getCellHeight() * BUTTER_FILL;
    float head = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + Math.max(zombieSpriteHeight(zombie), geometry.getCellHeight() * 0.6f) * 0.82f;
    float wobble = geometry.getCellHeight() * BUTTER_WOBBLE
        * (float) Math.sin(clock * 9f + zombie.getRow());
    stamp(context.getBatch(), butterRegion,
        geometry.columnCentreX(drawColumn(zombie)), head + wobble, height);
  }

  private void loadIceBlock() {
    if (iceBlockChecked) {
      return;
    }
    iceBlockChecked = true;
    iceBlockRegion = plantArt.findPart(ICE_BLOCK_RIG, ICE_BLOCK_REGION);
  }

  /**
   * The plant food a glowing zombie is carrying, drawn bobbing over its head.
   *
   * <p>Killing one is the only way to get plant food in most levels, so which zombie is holding it
   * has to be readable at a glance: the terminal build says "glowing" in its status line and the
   * graphical build said nothing at all. Drawn after the body so it is never hidden behind it.
   */
  private void drawPlantFoodCarrier(RenderContext context, Zombie zombie) {
    if (!zombie.isShiny()) {
      return;
    }
    TextureRegion icon = hudArt.find("plantfood");
    if (icon == null) {
      return;
    }
    float size = geometry.getCellHeight() * PLANT_FOOD_MARK_FRACTION;
    float bob = geometry.getCellHeight() * 0.04f * (float) Math.sin(clock * 3.2f);
    float x = geometry.columnCentreX(drawColumn(zombie)) - size / 2f;
    float y = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + zombieSpriteHeight(zombie) + bob;
    context.getBatch().setColor(1f, 1f, 1f, 0.95f);
    context.getBatch().draw(icon, x, y, size, size);
    context.getBatch().setColor(Color.WHITE);
  }

  private static float zombieAlpha(Zombie zombie) {
    return zombie.isSubmerged() ? 0.55f : 1f;
  }

  private void drawShadow(RenderContext context, TextureRegion shadow, double column, int row,
      float widthInLanes, float footInset) {
    float width = geometry.getCellHeight() * widthInLanes;
    float height = width * shadow.getRegionHeight() / (float) shadow.getRegionWidth();
    context.getBatch().draw(shadow,
        geometry.columnCentreX(column) - width / 2f,
        geometry.rowToY(row) + geometry.getCellHeight() * footInset - height / 2f,
        width, height);
  }

  private void spawnLootPickups(Board board) {
    for (LootDropper.LootSpawn spawn : board.drainPendingLootSpawns()) {
      hits.spawnPickup(spawn.kind(), spawn.column(), spawn.row());
    }
  }

  private void applyShake(RenderContext context, float delta, int freshDeaths) {
    if (freshDeaths > 0) {
      shakeMagnitude = Math.min(MAX_SHAKE, shakeMagnitude + freshDeaths * SHAKE_PER_DEATH);
    }
    OrthographicCamera camera = context.getCamera();
    float restX = camera.viewportWidth / 2f;
    float restY = camera.viewportHeight / 2f;
    if (shakeMagnitude <= 0.05f) {
      shakeMagnitude = 0f;
      camera.position.set(restX, restY, camera.position.z);
      return;
    }
    shakeSeed += delta * 46f;
    float dx = (float) Math.sin(shakeSeed * 12.9f) * shakeMagnitude;
    float dy = (float) Math.cos(shakeSeed * 17.3f) * shakeMagnitude;
    camera.position.set(restX + dx, restY + dy, camera.position.z);
    shakeMagnitude = Math.max(0f, shakeMagnitude - SHAKE_DECAY_PER_SECOND * delta);
  }

  private void drawSprites(RenderContext context, Board board, float delta) {
    muzzles.clear();
    zombieMuzzles.clear();
    context.getBatch().begin();
    drawGroundShadows(context, board);
    TextureRegion sheep = hudArt.find("sheep");
    for (int row = 0; row < board.getRows(); row++) {
      drawLawnRow(context, board, row, sheep, delta);
    }
    drawProjectiles(context, board);
    drawBossHazards(context, board);
    drawSuns(context, board);
    drawPlantFoodDrops(context, board);
    drawLootPickups(context);
    context.getBatch().setColor(Color.WHITE);
    drawBlasts(context);
    drawImpacts(context);
    drawDeaths(context);
    drawDebris(context);
    drawSparks(context);
    context.getBatch().setColor(Color.WHITE);
    context.getBatch().end();
  }

  /** One lane, in the order that sorts its plants, octopuses and zombies against each other. */
  private void drawLawnRow(RenderContext context, Board board, int row, TextureRegion sheep,
      float delta) {
    for (Plant plant : board.getPlants()) {
      if (plant.getRow() == row) {
        drawPlant(context, plant, sheep, delta,
            board.isWaterAt(plant.getRow(), plant.getCol()));
      }
    }
    // Its own pass over the row, so neither an octopus nor a block of ice is hidden under the
    // neighbouring plant drawn next, and both sit over the plant they have hold of.
    context.getBatch().setColor(Color.WHITE);
    for (Plant plant : board.getPlants()) {
      if (plant.getRow() == row) {
        drawPlantIceBlock(context, plant);
        drawOctopusHold(context, plant);
      }
    }
    context.getBatch().setColor(Color.WHITE);
    for (Zombie zombie : board.getZombies()) {
      if (footRow(zombie) == row && !zombie.isDead()) {
        drawZombie(context, zombie, delta);
      }
    }
    // In the row pass with the living, so a collapsing zombie sorts against its neighbours
    // instead of being painted over the whole lawn afterwards.
    drawTheFallen(context, row, delta);
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawSuns(RenderContext context, Board board) {
    TextureRegion sun = hudArt.find("sun");
    if (sun != null) {
      for (Sun s : board.getSuns()) {
        // the doc wants the sun kinds told apart; a big one is bigger and a radioactive one glows
        context.getBatch().setColor(
            s.getType() == SunType.RADIOACTIVE ? radioactiveSun : Color.WHITE);
        drawCentred(context, sun, s.getX(), lerp(s.getPreviousY(), s.getY(), tickAlpha),
            geometry.getCellHeight() * (s.getType() == SunType.LARGE ? 0.58f : 0.42f));
      }
      context.getBatch().setColor(Color.WHITE);
    }
    for (Sun s : board.getSuns()) {
      // the doc wants the sun kinds told apart; a big one is bigger and a radioactive one glows
      context.getBatch().setColor(
          s.getType() == SunType.RADIOACTIVE ? radioactiveSun : Color.WHITE);
      drawCentred(context, sun, s.getX(), lerp(s.getPreviousY(), s.getY(), tickAlpha),
          geometry.getCellHeight() * (s.getType() == SunType.LARGE ? 0.58f : 0.42f));
    }
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawLootPickups(RenderContext context) {
    for (HitEffects.LootPickup pickup : hits.getPickups()) {
      TextureRegion art = lootIcon(pickup.kind());
      if (art == null) {
        continue;
      }
      context.getBatch().setColor(1f, 1f, 1f, pickup.alpha());
      drawCentred(context, art, pickup.column(), pickup.row(),
          geometry.getCellHeight() * LOOT_ICON_FRACTION,
          geometry.getCellHeight() * LOOT_LIFT_FRACTION * pickup.progress(), 0f);
    }
  }

  /**
   * Missiles and boulders on their way down, and sharks on their way in.
   *
   * <p>A falling one is drawn above the tile it is aimed at and comes down onto it; the ring that
   * marks where it will land is a shape, and goes in with the other shapes.
   */
  private void drawBossHazards(RenderContext context, Board board) {
    float lane = geometry.getCellHeight();
    for (BossHazard hazard : board.getBossHazards()) {
      if (hazard.isFalling()) {
        TextureRegion art = hudArt.find(
            hazard.getKind() == BossHazard.Kind.ICE_BOULDER ? SNOW_REGION : PEA_REGION);
        if (art == null) {
          continue;
        }
        float lift = (float) (hazard.fallFraction() * BossHazard.FALL_HEIGHT_LANES) * lane;
        context.getBatch().setColor(hazard.getKind() == BossHazard.Kind.ICE_BOULDER
            ? Color.WHITE : missileTint);
        drawCentred(context, art, hazard.getColumn(), hazard.getRow(),
            lane * HAZARD_SIZE_LANES, lift, clock * -520f % 360f);
        context.getBatch().setColor(Color.WHITE);
        continue;
      }
      drawShark(context, hazard, lane);
    }
  }

  private void drawShark(RenderContext context, BossHazard hazard, float lane) {
    double column = lerp(hazard.getPreviousColumn(), hazard.getColumn(), tickAlpha);
    EntityAnimation rig = animations.find(AnimationLibrary.ZOMBIES, SHARK_RIG);
    float x = geometry.columnCentreX(column);
    float y = geometry.rowCentreY(hazard.getRow());
    // A little bob, so a shark reads as swimming rather than sliding along the tile.
    float bob = lane * 0.05f * (float) Math.sin(clock * 6.5f + hazard.getRow());
    if (rig != null && rig.drawLoosePart(context.getBatch(), x, y + bob,
        lane * SHARK_SIZE_LANES, true, 0f, SHARK_PART)) {
      return;
    }
    TextureRegion fallback = hudArt.find(SPLAT_REGION);
    if (fallback != null) {
      drawCentred(context, fallback, column, hazard.getRow(), lane * SHARK_SIZE_LANES);
    }
  }

  /**
   * The flash an explosive plant makes when it goes off.
   *
   * <p>Sized off the blast's own radius, so the ring the player sees is the area that was actually
   * damaged rather than a fixed puff -- a Cherry Bomb's 3x3 and a Jalapeno's whole lane look as
   * different as they are. Drawn before the impacts and the deaths so the ash and dust of the
   * zombies it killed land on top of it in the order they happen.
   */
  private void drawBlasts(RenderContext context) {
    TextureRegion flash = hudArt.find(BLAST_REGION);
    if (flash == null) {
      return;
    }
    float lane = geometry.getCellHeight();
    for (HitEffects.Blast blast : hits.getBlasts()) {
      float size = lane * blast.radiusLanes() * 2f;
      if (blast.fiery()) {
        context.getBatch().setColor(1f, 0.62f, 0.24f, blast.alpha());
      } else {
        context.getBatch().setColor(1f, 0.94f, 0.72f, blast.alpha());
      }
      drawCentred(context, flash, onBoard(blast.column()), blast.row(), size);
    }
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawImpacts(RenderContext context) {
    TextureRegion art = hudArt.find(SPLAT_REGION);
    if (art == null) {
      return;
    }
    for (HitEffects.Burst burst : hits.getBursts()) {
      float size = geometry.getCellHeight() * SPLAT_SIZE_LANES * (0.5f + 0.5f * burst.progress());
      context.getBatch().setColor(1f, 1f, 1f, burst.alpha());
      drawCentred(context, art, burst.column(), burst.row(), size);
    }
  }

  private void drawDeaths(RenderContext context) {
    TextureRegion cloud = hudArt.find(DUST_REGION);
    if (cloud == null) {
      return;
    }
    TextureRegion ash = hudArt.find(ASH_REGION);
    for (HitEffects.DeathPuff puff : hits.getDeathPuffs()) {
      float lane = geometry.getCellHeight();
      double column = onBoard(puff.column());
      // Ash is what is left of a zombie an explosion killed. One shot to pieces leaves dust.
      if (ash != null && puff.explosive()) {
        context.getBatch().setColor(1f, 1f, 1f, Math.min(1f, puff.alpha() * 1.4f));
        drawCentred(context, ash, column, puff.row(),
            lane * ASH_SIZE_LANES * (0.7f + 0.3f * puff.progress()),
            lane * ZOMBIE_FOOT_INSET, 0f);
      }
      context.getBatch().setColor(1f, 1f, 1f, puff.alpha() * 0.7f);
      drawCentred(context, cloud, column, puff.row(),
          lane * DUST_SIZE_LANES * (0.45f + 0.55f * puff.progress()),
          lane * (0.1f + 0.25f * puff.progress()), 0f);
    }
  }

  /** The pieces that came off, drawn over the corpses they came from. */
  private void drawDebris(RenderContext context) {
    float lane = geometry.getCellHeight();
    for (DetachedParts.Piece piece : debris.all()) {
      context.getBatch().setColor(1f, 1f, 1f, piece.alpha());
      piece.draw(context.getBatch(), geometry.columnCentreX(piece.column()),
          geometry.rowToY(piece.row()) + lane * (ZOMBIE_FOOT_INSET + piece.lift()),
          lane * piece.heightLanes());
    }
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawSparks(RenderContext context) {
    float lane = geometry.getCellHeight();
    for (HitEffects.Spark spark : hits.getSparks()) {
      TextureRegion art = sparkArt(spark.kind());
      if (art == null) {
        continue;
      }
      float grow = 0.45f + 0.55f * spark.progress();
      switch (spark.kind()) {
        case SPARK_PLANTED -> {
          context.getBatch().setColor(1f, 1f, 1f, spark.alpha() * 0.85f);
          drawCentred(context, art, spark.column(), spark.row(),
              lane * 0.16f * grow, lane * (PLANT_FOOT_INSET - 0.46f), 0f);
        }
        case SPARK_SUN -> {
          context.getBatch().setColor(1f, 0.95f, 0.6f, spark.alpha());
          drawCentred(context, art, spark.column(), spark.row(), lane * 0.6f * grow);
        }
        default -> {
          context.getBatch().setColor(1f, 1f, 1f, spark.alpha());
          drawCentred(context, art, spark.column(), spark.row(),
              lane * SPARK_SIZE_LANES * grow, lane * 0.28f, 0f);
        }
      }
    }
  }

  private TextureRegion sparkArt(String kind) {
    return switch (kind) {
      case SPARK_ARMOUR -> hudArt.find(ARMOUR_BREAK_REGION);
      case SPARK_PLANTED -> hudArt.find(DIRT_REGION);
      case SPARK_SUN -> hudArt.find(PLANT_PUFF_REGION);
      default -> null;
    };
  }

  private TextureRegion lootIcon(String kind) {
    return switch (kind) {
      case "coin" -> currencyArt.findCoin();
      case "diamond" -> currencyArt.findGem();
      case "pot" -> hudArt.find("pot");
      default -> null;
    };
  }

  private double onBoard(double column) {
    return Math.max(0.0, column);
  }


  private Color zombieTint(Zombie zombie) {
    if (zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      return frozenTint;
    }
    // Above the chill, because goo that is actively eating a zombie is the more urgent of the two
    // and the player has no other way to tell it is happening.
    if (zombie.getActiveEffects().containsKey(StatusEffect.POISONED)) {
      return poisonedTint;
    }
    if (zombie.getActiveEffects().containsKey(StatusEffect.CHILLED)) {
      return chilledTint;
    }
    if (zombie.isHypnotized()) {
      return hypnoTint;
    }
    // The doc asks for a reddish warning on a zombie close to the house, which is the last thing
    if (zombie.getX() <= NEAR_HOUSE_COLUMN) {
      return nearHouse;
    }
    return zombie.hasShieldBlocker() ? shieldTint : Color.WHITE;
  }

  private void observeForEffects(Board board) {
    for (Board.Blast blast : board.drainBlasts()) {
      hits.spawnBlast(blast.column(), blast.row(), blast.radius(), blast.fiery());
    }
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        hits.observe(plant, plant.getCurrentHealth());
        seenPlants.put(plant, Boolean.TRUE);
        noteNewPlant(plant);
      }
    }
    knownPlants.clear();
    knownPlants.putAll(seenPlants);
    // Dropped with the plant, so a long match does not keep a tick per plant ever planted.
    plantedAt.keySet().retainAll(seenPlants.keySet());
    seenPlants.clear();
    hits.forgetCounts(entity -> entity instanceof Zombie zombie && zombie.isDead());
    seenABoard = true;
    zombiesThisFrame.clear();
    for (Zombie zombie : board.getZombies()) {
      zombiesThisFrame.put(zombie, Boolean.TRUE);
      hits.observeZombieState(zombie, zombie.isDead(), zombie.getX(), footRow(zombie));
      if (!zombie.isDead()) {
        hits.observe(zombie, zombie.getCurrentHealth());
        hits.observeCount(zombie, intactArmour(zombie), SPARK_ARMOUR,
            onBoard(zombie.getX()), footRow(zombie));
        throwOffBrokenArmour(zombie);
      }
    }
    // What each zombie was wearing is remembered for exactly as long as the zombie is on the
    // board; anything not seen this frame has died or walked off and cannot break armour again.
    wornArmourGroups.keySet().removeIf(zombie -> !zombiesThisFrame.containsKey(zombie));
    pianoKeys.keySet().removeIf(zombie -> !zombiesThisFrame.containsKey(zombie));
    collectTheFallen();
    for (Projectile projectile : board.getProjectiles()) {
      hits.observeProjectile(projectile, projectile.getXCoordinate(),
          Math.round(projectile.getYCoordinate()));
    }
    noteCollectedSuns(board);
  }

  /**
   * Notices zombies that were on the board last frame and are not on it now.
   *
   * <p>Disappearing is the only signal there is, since the board never shows a dead zombie to a
   * renderer. Whether it died or simply left is then read straight off the object: a killed zombie
   * still says isDead(), while a hypnotised one that walked out the right-hand side does not, and
   * that one gets no death.
   */
  private void collectTheFallen() {
    for (Zombie zombie : zombiesLastFrame.keySet()) {
      if (zombiesThisFrame.containsKey(zombie) || !zombie.isDead()) {
        continue;
      }
      DyingZombie fallen = new DyingZombie(zombie, onBoard(zombie.getX()), footRow(zombie));
      dying.add(fallen);
      // The puff, the shake and the death sound all hang off this count, and none of them were
      // ever firing: the transition they waited on happens where nothing can see it.
      hits.spawnDeathPuff(fallen.column, fallen.row, zombie.wasKilledByBlast());
      throwOffBodyParts(fallen);
    }
    zombiesLastFrame.clear();
    zombiesLastFrame.putAll(zombiesThisFrame);
  }

  /**
   * Sends this zombie's head and one arm flying, and takes them off the corpse.
   *
   * <p>Both halves matter: throwing the gib without hiding the body's own head leaves the zombie
   * collapsing with two of them. Rigs with no gib -- the Zombosses, the Gargantuars, the Dodo --
   * keep their heads, which is how they are authored and how the game plays them.
   */
  private void throwOffBodyParts(DyingZombie fallen) {
    Zombie zombie = fallen.zombie;
    EntityAnimation rig = zombieAnimation(zombie);
    if (rig == null || zombie.isBoss()) {
      return;
    }
    boolean flip = zombie.isHypnotized();
    if (rig.hasPart(BodyParts.HEAD_GIB)) {
      debris.spawn(rig, fallen.column, fallen.row, HEAD_GIB_LIFT, HEAD_GIB_SIZE, flip, true,
          BodyParts.HEAD_GIB);
      fallen.headOff = true;
    }
    if (rig.hasPart(BodyParts.ARM_GIB)) {
      debris.spawn(rig, fallen.column, fallen.row, ARM_GIB_LIFT, ARM_GIB_SIZE, flip, false,
          BodyParts.ARM_GIB);
      fallen.armOff = true;
    }
  }

  /**
   * Throws off any armour this zombie has lost since the last frame.
   *
   * <p>The break itself was already being spotted -- it is what fires the armour spark -- but a
   * spark is a puff of dust where the doc asks for the piece itself to come off, and the rigs
   * carry every armour in each of its damage states. The piece thrown is the most damaged state
   * the rig has, since that is what the player was looking at when it gave way.
   */
  private void throwOffBrokenArmour(Zombie zombie) {
    if (zombie.getArmors().isEmpty()) {
      // Armour is fitted when the zombie is built and only ever destroyed, so a zombie with an
      // empty list never had any and never will. Skipping keeps the map to the few that do.
      return;
    }
    Set<String> worn = intactArmourGroups(zombie);
    Set<String> before = wornArmourGroups.put(zombie, worn);
    if (before == null || worn.containsAll(before)) {
      return;
    }
    EntityAnimation rig = zombieAnimation(zombie);
    if (rig == null) {
      return;
    }
    for (String group : before) {
      if (worn.contains(group)) {
        continue;
      }
      String[] states = BodyParts.armourStateParts(rig.partNames(), group);
      if (states.length > 0) {
        debris.spawn(rig, onBoard(zombie.getX()), footRow(zombie), ARMOUR_GIB_LIFT,
            ARMOUR_GIB_SIZE, zombie.isHypnotized(), true, states);
      }
    }
  }

  private static Set<String> intactArmourGroups(Zombie zombie) {
    Set<String> groups = new java.util.LinkedHashSet<>();
    for (Armor armor : zombie.getArmors()) {
      if (armor == null || armor.isDestroyed() || armor.getType() == null) {
        continue;
      }
      String group = switch (armor.getType()) {
        case CONE -> ArmourParts.CONE;
        case BUCKET -> ArmourParts.BUCKET;
        case BLOCK -> ArmourParts.BRICK;
        case HELMET -> ArmourParts.CROWN;
        case SHOULDER_ARMOR -> ArmourParts.SHOULDER;
        default -> null;
      };
      if (group != null) {
        groups.add(group);
      }
    }
    return groups;
  }

  /** Ages the fallen and drops the ones whose clip has played out. */
  private void advanceTheFallen(float delta) {
    for (java.util.Iterator<DyingZombie> it = dying.iterator(); it.hasNext(); ) {
      DyingZombie fallen = it.next();
      fallen.age += delta;
      if (fallen.age >= deathClipSeconds(fallen)) {
        // AnimationStates sweeps anything it was not asked to advance this frame, so dropping the
        // corpse here is enough to let its playback entry go too.
        it.remove();
      }
    }
  }

  /** How long this zombie's own die clip runs, or a short default when its rig has none. */
  private float deathClipSeconds(DyingZombie fallen) {
    EntityAnimation animation = zombieAnimation(fallen.zombie);
    if (animation == null) {
      return HitEffects.DEATH_SECONDS;
    }
    String clip = animation.pickClip("die", "die2");
    float duration = clip == null ? 0f : animation.duration(clip);
    return duration > 0f ? duration : HitEffects.DEATH_SECONDS;
  }

  /** Draws the zombies that are still collapsing in this row. */
  private void drawTheFallen(RenderContext context, int row, float delta) {
    for (DyingZombie fallen : dying) {
      if (fallen.row != row) {
        continue;
      }
      EntityAnimation animation = zombieAnimation(fallen.zombie);
      String clip = animation == null ? null : animation.pickClip("die", "die2");
      if (clip == null) {
        continue;
      }
      context.getBatch().setColor(Color.WHITE);
      animation.draw(context.getBatch(), clip, playback.advance(fallen, clip, delta),
          geometry.columnCentreX(fallen.column),
          geometry.rowToY(fallen.row) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET,
          zombieAnimationScale(fallen.zombie, animation,
              zombieAnchorClip(animation, fallen.zombie)),
          fallen.zombie.isHypnotized(), corpseVisibility(animation, fallen));
    }
  }

  private static int intactArmour(Zombie zombie) {
    int intact = 0;
    for (Armor armor : zombie.getArmors()) {
      if (armor != null && !armor.isDestroyed()) {
        intact++;
      }
    }
    return intact;
  }

  private void noteNewPlant(Plant plant) {
    if (knownPlants.containsKey(plant)) {
      return;
    }
    // Remembered whether or not the spark is drawn, because the mints' intro clip is timed off
    // it: Plant only stamps its own plantedTick for the few plants that expire on a timer.
    plantedAt.putIfAbsent(plant, currentTick);
    if (seenABoard) {
      hits.spawnSpark(SPARK_PLANTED, plant.getCol(), plant.getRow());
    }
  }

  private void noteCollectedSuns(Board board) {
    for (Sun sun : board.getSuns()) {
      seenSuns.put(sun, new double[] {sun.getX(), sun.getY()});
    }
    knownSuns.entrySet().removeIf(entry -> {
      if (seenSuns.containsKey(entry.getKey())) {
        return false;
      }
      hits.spawnSpark(SPARK_SUN, entry.getValue()[0], (int) Math.round(entry.getValue()[1]));
      return true;
    });
    knownSuns.putAll(seenSuns);
    seenSuns.clear();
  }

  private Color flashed(Color base, Object entity) {
    float strength = hits.flashStrength(entity);
    if (strength <= 0f) {
      return base;
    }
    return tinted.set(base).lerp(hitFlash, strength);
  }

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

  private boolean drawPlantAnimation(RenderContext context, Plant plant, float delta,
      boolean onWater) {
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plant.getName());
    String clip = animation == null ? null : plantClip(animation, plant, onWater);
    if (clip == null) {
      return false;
    }
    float fuse = fuseTime(plant, animation, clip);
    float time = fuse >= 0f
        ? playback.hold(plant, clip, fuse)
        : playback.advance(plant, clip, delta);
    float x = geometry.columnCentreX(plant.getCol());
    float y = geometry.rowToY(plant.getRow()) + geometry.getCellHeight() * PLANT_FOOT_INSET;
    // Sized and placed by the plant's resting clip, not the one playing. Both are taken from the
    // clip's own box, and the boxes differ enough between a plant standing and a plant shooting
    // that reading them off the playing clip made the plant lurch forward and shrink on the shot.
    // The halo-wide plants still skip the clamp entirely: their boxes are mostly glow whichever
    // clip is measured, so anchoring alone would not bring them back to size.
    String anchor = anchorClip(animation, plant);
    float scale = HALO_WIDE_PLANTS.contains(plant.getName())
        ? geometry.getCellHeight() * PLANT_ROW_FILL / PLANT_ANIM_UNITS
        : scaleFor(animation.width(anchor), PLANT_ANIM_UNITS, PLANT_ROW_FILL);
    animation.draw(context.getBatch(), clip, anchor, time, x, y, scale, false, null);
    noteMuzzle(plant, animation, clip, anchor, time, x, y, scale);
    return true;
  }

  /**
   * The clip a plant is sized and placed by: its idle for whatever growth stage it is at.
   *
   * <p>Growth stages are kept apart because a Sun-shroom really is smaller at stage one, and that
   * is a change in the plant rather than a wobble from swapping clips.
   */
  private String anchorClip(EntityAnimation animation, Plant plant) {
    int stage = growthStage(plant);
    String idle = animation.pickClip(withStage(new String[] {"idle"}, stage));
    return idle != null ? idle : animation.pickClip("idle", "loop");
  }

  /**
   * Where in its explode clip a plant with a burning fuse should be drawn, or -1 for a plant that
   * is not one.
   *
   * <p>Stretched so the clip runs once across the whole fuse: the first frame lands on the tick
   * the plant went in and the last on the tick it detonates, whatever the rig's own clip length
   * happens to be. Driven off the fuse rather than the frame clock, so it cannot drift out of step
   * with the blast the model schedules.
   */
  private float fuseTime(Plant plant, EntityAnimation animation, String clip) {
    if (!(plant.getBehavior() instanceof model.game.plant.behavior.ExplodeAction fusing)) {
      return -1f;
    }
    double burnt = fusing.fuseProgress(plant, currentTick);
    if (burnt < 0) {
      return -1f;
    }
    float length = animation.duration(clip);
    return length <= 0f ? -1f : (float) burnt * length;
  }

  /**
   * Records where this plant's shots should leave it, taken from the head of the rig that was just
   * drawn rather than from a hand-written offset per plant, so it follows the animation instead of
   * drifting away from it. The release is how far into the firing tick the shot leaves the mouth,
   * read off the length of the clip that is playing and kept inside the tick it was fired on so
   * the shot is never held back past the tick that already dealt its damage.
   */
  private void noteMuzzle(Plant plant, EntityAnimation animation, String clip, String anchor,
      float time, float x, float y, float scale) {
    if (geometry.getCellWidth() <= 0f) {
      return;
    }
    // Same anchor the plant was drawn with, or the muzzle is measured against a body that is not
    // where the body actually is and the shot leaves from beside the plant.
    float[] head = animation.topPartBox(clip, anchor, time, x, y, scale, false);
    if (head == null) {
      return;
    }
    float forward = (head[0] + head[2] * MUZZLE_HEAD_REACH - x) / geometry.getCellWidth();
    float lift = head[1] - geometry.rowCentreY(plant.getRow());
    float lane = geometry.getCellHeight();
    muzzles.put(plant, new float[] {
        clamp(forward, -MUZZLE_MAX_FORWARD, MUZZLE_MAX_FORWARD),
        clamp(lift, -lane * MUZZLE_MAX_LIFT_LANES, lane * MUZZLE_MAX_LIFT_LANES),
        clamp(animation.duration(clip) * MUZZLE_RELEASE_OF_CLIP / GdxConfig.SECONDS_PER_TICK,
            MUZZLE_MIN_RELEASE, MUZZLE_MAX_RELEASE)});
  }

  private static float clamp(float value, float low, float high) {
    return value < low ? low : Math.min(value, high);
  }

  /** Where to draw a zombie: between the tile it stood on last tick and the one it is on now. */
  private double drawColumn(Zombie zombie) {
    return onBoard(lerp(zombie.getPreviousX(), zombie.getX(), tickAlpha));
  }

  private static final Map<String, String[]> ACTION_CLIP_NAMES = Map.ofEntries(
      Map.entry("Chomper", new String[] {"bite"}),
      Map.entry("Magnet-shroom", new String[] {"catch", "busy"}),
      Map.entry("Squash",
          new String[] {"jump_down_left", "jump_down_right", "jump_up_left", "jump_up_right"}),
      Map.entry("Fume-shroom", new String[] {"special"}),
      Map.entry("Sun-shroom", new String[] {"special"}),
      // The sun producers: their rigs call the "here is a sun" bob "special", not "attack", so
      // without naming it they fell through to idle and never visibly produced anything.
      Map.entry("Sunflower", new String[] {"special"}),
      Map.entry("Twin Sunflower", new String[] {"special"}),
      Map.entry("Primal Sunflower", new String[] {"special"}),
      // Potato Mine arms rather than attacks: "plant" is it burying in, "plant_idle" the armed
      // wait, and "attack" only happens when something finally steps on it.
      Map.entry("Potato Mine", new String[] {"attack"}),
      Map.entry("Primal Potato Mine", new String[] {"attack"}));

  /**
   * Which clip a plant is showing this frame, in priority order: plant food, then the arming pose
   * of a mine, then its attack while that is still running, then its idle -- damage-staged for the
   * plants whose rigs wear down as they are eaten.
   */
  private String plantClip(EntityAnimation animation, Plant plant, boolean onWater) {
    int stage = growthStage(plant);

    // A lit fuse outranks everything else: whatever else the plant might be showing, it is about
    // to go off, and the clip for that has to be running from the tick it went in.
    if (plant.getBehavior() instanceof model.game.plant.behavior.ExplodeAction fusing
        && fusing.fuseProgress(plant, currentTick) >= 0) {
      String blast = animation.pickClip(EXPLODE_CLIPS);
      if (blast != null) {
        return blast;
      }
    }

    // Plant food outranks everything: it is a one-off dose and the clip is the whole point of it.
    if (plant.isPlantFoodActive()) {
      String food = animation.pickClip(withStage(PLANT_FOOD_CLIPS, stage));
      if (food != null) {
        return food;
      }
    }

    // The mints' entry animation. Their rigs are intro/loop/outro with no idle at all, and the
    // doc asks for the intro on top of the idle, so it runs once for its own length from the tick
    // the plant went down and then hands over to the loop below.
    String intro = introClip(animation, plant);
    if (intro != null) {
      return intro;
    }

    String[] names = ACTION_CLIP_NAMES.getOrDefault(plant.getName(), new String[] {"attack"});
    String attack = animation.pickClip(withStage(names, stage));
    if (attack != null && justActed(plant, animation.duration(attack)) && !isWaitingTrap(plant)) {
      return attack;
    }

    // An armed mine is buried and waiting, which is a different pose from a plant standing about.
    if (isArmedMine(plant)) {
      String armed = animation.pickClip("plant_idle", "plant");
      if (armed != null) {
        return armed;
      }
    }

    // "loop" last, for the mints: their standing-about clip is the only one they have and it is
    // not called idle, so without it they resolved to no clip at all and fell back to a still.
    String[] idleNames = stage > 0
        ? new String[] {"idle_stage" + stage, "idle", "loop"}
        : new String[] {"idle", "loop"};
    return animation.pickClip(concat(damageStageClips(plant), concat(idleNames, names)));
  }

  /**
   * The one-shot clip a plant plays as it goes down, while it is still running, or null.
   *
   * <p>Only asked for by name: a rig without an {@code intro} gets nothing, so this cannot pull a
   * plant off the clip it should be showing.
   */
  private String introClip(EntityAnimation animation, Plant plant) {
    if (!animation.hasClip(PLANT_INTRO_CLIP)) {
      return null;
    }
    Integer planted = plantedAt.get(plant);
    if (planted == null) {
      return null;
    }
    float ticks = animation.duration(PLANT_INTRO_CLIP) * GdxConfig.TICKS_PER_SECOND;
    return currentTick - planted < ticks ? PLANT_INTRO_CLIP : null;
  }

  /** The clip a plant with a lit fuse plays, most specific first. */
  private static final String[] EXPLODE_CLIPS =
      {"attack", "explode", "stage3_explode", "stage2_explode", "stage1_explode"};

  /** Clip names for a plant-food dose, most specific first. */
  // "pf" last, and it is the Sea-shroom's: its rig is the one that abbreviates the name, so its
  // plant-food dose was the only one in the roster playing no animation at all. Checked against
  // every plant rig -- no other clip name anywhere contains "pf", so the loose match cannot stray.
  private static final String[] PLANT_FOOD_CLIPS =
      {"plantfood", "plantfood_on", "plantfood_loop", "pf"};

  /** The mints' entry animation, and the only rigs in the library that carry one. */
  private static final String PLANT_INTRO_CLIP = "intro";

  /** Plants whose rigs carry a wear-down sequence instead of only one idle. */
  private static final Set<String> DAMAGE_STAGE_PLANTS =
      Set.of("Wall-nut", "Tall-nut", "Garlic", "Explode-o-nut", "Endurian");

  /**
   * Plants wrapped in a halo far wider than the plant inside it, which skip the lane width clamp.
   *
   * <p>The clamp shrinks a rig until its clip bounds fit one lane, and for these the bounds are
   * mostly soft glow or flame rather than plant: the Iceberg Lettuce came out at 43% and the
   * Magnet-shroom at 42% of the scale every other plant is drawn at. Named one by one on purpose --
   * the halo layers cannot be told apart from the body by name, since the same glow image hangs
   * both under a labelled node and directly under the root.
   *
   * <p>Torchwood and Fire Peashooter joined the list after the audit: Torchwood's idle box is
   * mostly the fire it carries and Fire Peashooter's is mostly its flame, so the clamp was reading
   * the flame as plant width and shrinking both well below their neighbours on the lawn.
   */
  private static final Set<String> HALO_WIDE_PLANTS =
      Set.of("Ice-shroom", "Magnet-shroom", "Iceberg Lettuce", "Torchwood", "Fire Peashooter");

  /**
   * The wear-down clips for a defensive plant, worst damage first, or nothing for a plant whose
   * rig has none.
   *
   * <p>Wall-nut ships damage/damage2/damage3 and Garlic idle_damage/idle_damage2; both are the
   * same idea, that a nut being eaten should look chewed rather than pristine right up to the
   * moment it disappears. Thirds of max health, so the two- and three-stage rigs both read.
   */
  private static String[] damageStageClips(Plant plant) {
    if (!DAMAGE_STAGE_PLANTS.contains(plant.getName()) || plant.getMaxHealth() <= 0) {
      return new String[0];
    }
    float left = plant.getCurrentHealth() / (float) plant.getMaxHealth();
    if (left > 0.66f) {
      return new String[0];
    }
    return left > 0.33f
        ? new String[] {"damage", "idle_damage"}
        : new String[] {"damage3", "damage2", "idle_damage2", "damage", "idle_damage"};
  }

  /** True for a mine that is buried and waiting rather than still burrowing in. */
  private static boolean isArmedMine(Plant plant) {
    return plant.getBehavior() instanceof model.game.plant.behavior.ExplodeAction armed
        && armed.isArmed();
  }

  /**
   * A trap lying in wait, which should be showing its idle rather than its attack.
   *
   * <p>A contact trap stamps its action tick once, when it arms, and never again -- so Squash was
   * playing its pounce for the first eight ticks after it was planted, on a lawn where nothing had
   * happened yet, and then standing still through the one moment the pounce is for. The clip is
   * shorter than the window it was held for, so it wrapped and pounced twice on the spot.
   */
  private static boolean isWaitingTrap(Plant plant) {
    return plant.getBehavior() instanceof model.game.plant.behavior.ExplodeAction trap
        && trap.isContactTrap() && !trap.hasDetonated();
  }

  private static int growthStage(Plant plant) {
    return plant.getBehavior() instanceof model.game.plant.behavior.GrowthStageAction growth
        ? growth.getCurrentStage()
        : 0;
  }

  private static String[] withStage(String[] names, int stage) {
    return stage > 0 ? concat(stageSuffixed(names, stage), names) : names;
  }

  private static String[] stageSuffixed(String[] names, int stage) {
    String[] out = new String[names.length];
    for (int i = 0; i < names.length; i++) {
      out[i] = names[i] + "_stage" + stage;
    }
    return out;
  }

  private static String[] concat(String[] first, String[] second) {
    String[] out = new String[first.length + second.length];
    System.arraycopy(first, 0, out, 0, first.length);
    System.arraycopy(second, 0, out, first.length, second.length);
    return out;
  }

  private boolean justActed(Plant plant, float attackSeconds) {
    int sinceAction = currentTick - plant.getLastActionTick();
    if (plant.getLastActionTick() <= 0 || sinceAction < 0) {
      return false;
    }
    int holdTicks = attackSeconds > 0f
        ? Math.round(attackSeconds * GdxConfig.TICKS_PER_SECOND)
        : PLANT_ATTACK_HOLD_TICKS;
    return sinceAction < holdTicks;
  }

  private boolean drawZombieAnimation(RenderContext context, Zombie zombie, float delta) {
    EntityAnimation animation = zombieAnimation(zombie);
    if (animation == null) {
      return false;
    }
    String clip = zombieClip(animation, zombie);
    float time = playback.advance(zombie, clip, delta * animationRate(zombie, clip));
    float flight = zombie.flightProgress();
    double column = flight > 0f
        ? onBoard(zombie.getX() + (zombie.getThrownFromX() - zombie.getX()) * flight)
        : drawColumn(zombie);
    float x = geometry.columnCentreX(column);
    float y = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + throwLift(flight);
    String anchor = zombieAnchorClip(animation, zombie);
    float scale = zombieAnimationScale(zombie, animation, anchor);
    animation.draw(context.getBatch(), clip, anchor, time, x, y, scale, zombie.isHypnotized(),
        armourVisibility(animation, zombie));
    drawPlantHead(context, zombie, animation, clip, anchor, time, x, y, scale);
    drawPiano(context, zombie, clip, delta, x, y, scale);
    noteZombieMuzzle(zombie, animation, clip, anchor, time, x, y, scale);
    return true;
  }

  /** The rig name of the piano itself, which is a separate animation from the zombie pushing it. */
  private static final String PIANO_RIG = "ZombiePiano";

  /**
   * The Pianist's piano, drawn over the zombie that is playing it.
   *
   * <p>Upstream ships these as two rigs -- ZOMBIE_PIANO is the player, PIANO is the instrument --
   * and only the player was ever loaded, so the Pianist walked the lawn miming at nothing. The two
   * share one atlas page and one canvas, so the piano stands on the same spot at the same scale.
   */
  private void drawPiano(RenderContext context, Zombie zombie, String zombieClip, float delta,
      float x, float y, float scale) {
    if (!(zombie.getBehavior() instanceof PianistZombieAction)) {
      return;
    }
    EntityAnimation piano = animations.find(AnimationLibrary.ZOMBIES, PIANO_RIG);
    if (piano == null) {
      return;
    }
    // The instrument follows the player: both rigs ship idle, play and die, so asking for the
    // zombie's own clip first keeps the keys moving when the zombie is playing and still when it
    // is not.
    String clip = piano.pickClip(zombieClip, "play", "idle");
    if (clip == null) {
      return;
    }
    float time = playback.advance(pianoOf(zombie), clip, delta * animationRate(zombie, clip));
    piano.draw(context.getBatch(), clip, clip, time, x, y, scale, zombie.isHypnotized(), null);
  }

  /**
   * A per-zombie key for the piano's own playback, so two Pianists on the lawn do not share one
   * clip position and the piano is not advanced on the zombie's own state.
   */
  private Object pianoOf(Zombie zombie) {
    return pianoKeys.computeIfAbsent(zombie, z -> new Object());
  }

  private final Map<Zombie, Object> pianoKeys = new java.util.IdentityHashMap<>();

  /**
   * The same muzzle note the shooting plants get, for the zombies that fire something.
   *
   * <p>Their shots had none, so a Zombotany pea or a Hunter's ice blinked into being a tile from
   * the zombie the instant it was fired, with the throw still playing behind it. Mirrored rather
   * than shared because a zombie faces the other way, so its mouth is on its left.
   */
  private void noteZombieMuzzle(Zombie zombie, EntityAnimation animation, String clip,
      String anchor, float time, float x, float y, float scale) {
    if (geometry.getCellWidth() <= 0f) {
      return;
    }
    float[] head = animation.topPartBox(clip, anchor, time, x, y, scale, zombie.isHypnotized());
    if (head == null) {
      return;
    }
    float mouth = zombie.isHypnotized()
        ? head[0] + head[2] * MUZZLE_HEAD_REACH
        : head[0] + head[2] * (1f - MUZZLE_HEAD_REACH);
    float lane = geometry.getCellHeight();
    zombieMuzzles.put(zombie, new float[] {
        clamp((mouth - x) / geometry.getCellWidth(), -MUZZLE_MAX_FORWARD, MUZZLE_MAX_FORWARD),
        clamp(head[1] - geometry.rowCentreY(zombie.getRow()),
            -lane * MUZZLE_MAX_LIFT_LANES, lane * MUZZLE_MAX_LIFT_LANES),
        clamp(animation.duration(clip) * MUZZLE_RELEASE_OF_CLIP / GdxConfig.SECONDS_PER_TICK,
            MUZZLE_MIN_RELEASE, MUZZLE_MAX_RELEASE)});
  }

  private float throwLift(float flight) {
    if (flight <= 0f) {
      return 0f;
    }
    return geometry.getCellHeight() * THROW_ARC_HEIGHT * 4f * flight * (1f - flight);
  }

  private void drawPlantHead(RenderContext context, Zombie zombie, EntityAnimation body,
      String clip, String anchor, float time, float x, float y, float scale) {
    String plant = ZombieArt.zombotanyPlant(zombie.getName());
    if (plant == null) {
      return;
    }
    EntityAnimation rig = animations.find(AnimationLibrary.PLANTS, plant);
    String idle = rig == null ? null : rig.pickClip("idle");
    // Measured against the same anchor the body was drawn with, so the head rides the shoulders.
    float[] head = body.topPartBox(clip, anchor, time, x, y, scale, zombie.isHypnotized());
    if (idle == null || head == null) {
      return;
    }
    float span = Math.max(head[2], head[3]) * ZOMBOTANY_HEAD_FILL;
    float headHeight = rig.height(idle);
    if (headHeight <= 0f) {
      return;
    }
    rig.draw(context.getBatch(), idle, time, head[0], head[1] - span / 2f,
        span / headHeight, !zombie.isHypnotized());
  }

  /**
   * The dying zombie's armour, minus whatever came off it.
   *
   * <p>A part forced false takes its children with it, which is exactly right here: hiding the
   * skull hides the jaw and the eyes hanging off it.
   */
  private static Map<String, Boolean> corpseVisibility(EntityAnimation animation,
      DyingZombie fallen) {
    Map<String, Boolean> armour = armourVisibility(animation, fallen.zombie);
    if (!fallen.headOff && !fallen.armOff) {
      return armour;
    }
    Map<String, Boolean> visibility = armour == null ? new HashMap<>() : new HashMap<>(armour);
    for (String part : animation.partNames()) {
      String lower = part.toLowerCase();
      if ((fallen.headOff && BodyParts.isHeadPart(lower))
          || (fallen.armOff && BodyParts.isOuterArmPart(lower))) {
        visibility.put(part, false);
      }
    }
    return visibility;
  }

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
      boolean show = worn != null && stageMatches(lower, worn);
      visibility.put(part, show);
      any |= show;
    }
    return any ? visibility : null;
  }

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
        default -> "";
      })) {
        return armor;
      }
    }
    return null;
  }

  private static boolean stageMatches(String lowerPartName, Armor armor) {
    float left = armor.getCurrentHealth() / (float) Math.max(1, armor.getMaxHealth());
    boolean isStage1 = lowerPartName.contains("damage_01") || lowerPartName.endsWith("damaged1");
    boolean isStage2 = lowerPartName.contains("damage_02") || lowerPartName.endsWith("damaged2");
    if (!isStage1 && !isStage2) {
      return lowerPartName.contains("states") || left > ARMOUR_STAGE_1;
    }
    return isStage1 ? left <= ARMOUR_STAGE_1 && left > ARMOUR_STAGE_2 : left <= ARMOUR_STAGE_2;
  }

  private EntityAnimation zombieAnimation(Zombie zombie) {
    EntityAnimation animation = animations.find(AnimationLibrary.ZOMBIES, zombie.getName());
    if (animation == null && ZombieArt.zombotanyPlant(zombie.getName()) != null) {
      animation = animations.find(AnimationLibrary.ZOMBIES, ZOMBOTANY_BODY);
    }
    return animation != null && zombieClip(animation, zombie) != null ? animation : null;
  }

  private static String bossClip(EntityAnimation animation, Zombie zombie) {
    if (!(zombie.getBehavior() instanceof ZombossAction boss)) {
      return null;
    }
    switch (boss.getPose()) {
      case STUNNED:
        return animation.pickClip("stun_loop", "stun", "stun_start");
      case MOVING:
        return animation.pickClip("walk_forward", "walk", "idle");
      case ATTACKING:
        // fire_bomb is the Dark Ages dragon's, and it was the one attack with no clip of its own.
        return animation.pickClip("missile_start", "fire_attack", "fire_bomb", "suction_loop",
            "slingshot", "rocket_launch", "idle");
      case SUMMONING:
        // Each chapter's rig calls it something different: Egypt tears open a portal, the pirate
        // has a plain spawn, and the mammoth raises a column of ice and the wind that comes with
        // it. All of them were sitting unused while the boss summoned in its idle pose.
        return animation.pickClip("zombie_portal_start", "zombie_portal_loop", "spawn",
            "glacier_column_1", "wind_1", "idle");
      default:
        return animation.pickClip("idle");
    }
  }

  private static final int ACTION_POSE_TICKS = 8;

  /**
   * The pose a zombie holds for a moment after one of its set pieces -- the Jester's spin, the
   * Fisherman's cast, the Gargantuar's throw -- or null when it is doing none of them.
   */
  private String actionPoseClip(EntityAnimation animation, Zombie zombie) {
    if (zombie.getBehavior() instanceof JesterZombieAction jester && jester.isSpinning()) {
      String spin = animation.pickClip("spin_walk", "spin");
      if (spin != null) {
        return spin;
      }
    }
    if (zombie.getBehavior() instanceof HookPullZombieAction hook
        && currentTick - hook.getLastHookTick() < ACTION_POSE_TICKS) {
      String cast = animation.pickClip("toss", "cast", "reel");
      if (cast != null) {
        return cast;
      }
    }
    if (zombie.getBehavior() instanceof GargantuarAction gargantuar
        && gargantuar.getThrowTick() >= 0
        && currentTick - gargantuar.getThrowTick() < ACTION_POSE_TICKS) {
      // Not smash_left: that is the pole coming down, and letting the throw take it left the
      // Gargantuar with one pose for two different things and no smash of its own.
      String throwing = animation.pickClip("fire", "cannon_fire", "particles");
      if (throwing != null) {
        return throwing;
      }
    }
    return null;
  }

  private String zombieClip(EntityAnimation animation, Zombie zombie) {
    if (zombie.isBoss()) {
      String boss = bossClip(animation, zombie);
      if (boss != null) {
        return boss;
      }
    }
    String suffix = propSuffix(zombie);
    if (zombie.isEating()) {
      // A Gargantuar has no bite. It stops at a plant and brings the pole down on it, and its rig
      // ships smash_left for that; playing "eat" had it gumming the plant instead.
      String[] eatNames = zombie.getBehavior() instanceof GargantuarAction
          ? new String[] {"smash_left", "eat" + suffix, "eat"}
          : new String[] {"eat" + suffix, "eat"};
      String eat = animation.pickClip(eatNames);
      if (eat != null) {
        return eat;
      }
    }
    String held = actionPoseClip(animation, zombie);
    if (held != null) {
      return held;
    }
    // The rest of the roster's signature moves. Every one of these rigs ships a clip for the
    // thing its behaviour does -- the All-Star's kick, the Turquoise's sun-drain, the Tomb
    // Raiser's summon, the Hunter's throw, the Octopus thrower's toss -- and none of them were
    // ever asked for, so the ability happened with the zombie still plainly walking.
    String ability = abilityClip(animation, zombie);
    if (ability != null) {
      return ability;
    }
    // "play" is the Pianist: his rig has no walk cycle because the piano-playing loop is how he
    return animation.pickClip("walk" + suffix, "walk", "play", "idle" + suffix, "idle");
  }

  /**
   * The clip for a zombie that has just used its own special ability, or null.
   *
   * <p>Each behaviour records the tick it last acted on; the pose is held for
   * {@link #ACTION_POSE_TICKS} after that, the same way the hook and the imp throw already are.
   */
  private String abilityClip(EntityAnimation animation, Zombie zombie) {
    Object behavior = zombie.getBehavior();
    int actedAt;
    String[] names;
    if (behavior instanceof TacklerZombieAction tackler) {
      actedAt = tackler.getLastTackleTick();
      // "push" is the Arcade zombie's: it shoves its cabinet rather than kicking, and its rig has
      // no kick or tackle at all, so it was the one tackler that flattened plants while walking.
      names = new String[] {"kick", "tackle", "push"};
    } else if (behavior instanceof TurquoiseZombieAction turquoise) {
      actedAt = turquoise.getLastStealTick();
      names = new String[] {"power", "power_up"};
    } else if (behavior instanceof TombRaiserZombieAction raiser) {
      actedAt = raiser.getLastRaiseTick();
      names = new String[] {"power"};
    } else if (behavior instanceof HunterZombieAction hunter) {
      actedAt = hunter.getLastThrowTick();
      names = new String[] {"throw"};
    } else if (behavior instanceof OctopusThrowerZombieAction thrower) {
      actedAt = thrower.getLastThrowTick();
      names = new String[] {"toss"};
    } else if (behavior instanceof WizardZombieAction wizard) {
      // The Wizard rig ships a "sheep" clip -- the spell it casts -- and nothing ever played it,
      // so plants turned into sheep with the Wizard still plainly walking.
      actedAt = wizard.getLastCurseTick();
      names = new String[] {"sheep", "cast"};
    } else {
      return null;
    }
    if (actedAt < 0 || currentTick - actedAt >= ACTION_POSE_TICKS) {
      return null;
    }
    return animation.pickClip(names);
  }

  private static String propSuffix(Zombie zombie) {
    if (zombie.getBehavior() instanceof EnrageOnArmorBreakZombieAction enrage
        && !enrage.isEnraged()) {
      return "_newspaper";
    }
    if (zombie.getBehavior() instanceof BarrelRollerZombieAction barrel
        && !barrel.isBarrelBurst()) {
      return "2";
    }
    return "";
  }

  /**
   * How fast to play a zombie's clip. A walk cycle is tied to how far the zombie actually travels
   * per tick, so a chilled one plods, an enraged Newspaper's legs keep up with its sprint and an
   * All-Star that has slowed to a crawl stops skating over the lawn. Everything else -- eating,
   * dying, a special pose -- keeps its own pace apart from the chill.
   */
  private static float animationRate(Zombie zombie, String clip) {
    // Stunned as well as frozen: either way the zombie is not moving, so nor is its rig.
    if (zombie.isHeldStill()) {
      return 0f;
    }
    if (isWalkClip(clip)) {
      return (float) zombie.getStrideFraction();
    }
    return zombie.getActiveEffects().containsKey(StatusEffect.CHILLED) ? CHILLED_ANIM_RATE : 1f;
  }

  private static boolean isWalkClip(String clip) {
    return clip != null && clip.toLowerCase().contains("walk");
  }

  /**
   * The clip a zombie is sized and placed by: whatever it looks like walking.
   *
   * <p>Same reason plants are anchored to their idle. A clip's box is measured across all of its
   * frames, and eating, dying and every signature move have boxes of their own, so reading the
   * box off whatever is playing resized and shifted the zombie every time it changed what it was
   * doing. The prop suffix comes along so a Newspaper zombie stays measured against the clip it
   * is actually walking with.
   */
  private static String zombieAnchorClip(EntityAnimation animation, Zombie zombie) {
    String suffix = propSuffix(zombie);
    String walk = animation.pickClip("walk" + suffix, "walk", "idle" + suffix, "idle");
    return walk != null ? walk : animation.pickClip("idle");
  }

  private float zombieAnimationScale(Zombie zombie, EntityAnimation animation, String clip) {
    float height = animation.height(clip);
    if (isBoss(zombie)) {
      return height > 0f ? geometry.getCellHeight() * ZOMBOSS_ROW_FILL / height : 0f;
    }
    return zombieScaleFrom(animation.width(clip), height, ZOMBIE_ANIM_UNITS);
  }

  /**
   * Where the top of a zombie's rig actually is on screen right now, in world Y, for the health
   * bar to sit just above.
   *
   * <p>Not animation.height(clip) * scale: that height is the clip's bounding box aggregated
   * across every frame of it, since {@link EntityAnimation#draw} needs a stable value to anchor a
   * clip's feet consistently as it plays. A walk cycle's most stretched-out stride, or the
   * Gargantuar's pole raised overhead mid-smash, are both real frames of those clips and both
   * count toward it -- so a health bar built from that number sits at the height of the tallest
   * moment the clip ever reaches, with a visible gap above the character for every calmer frame in
   * between. {@link EntityAnimation#topPartBox} already solves this correctly for
   * {@link #drawPlantHead}, reading the part that is actually highest in the exact frame on
   * screen; this asks it the same question and reads the state {@link #drawZombieAnimation}
   * already advanced this frame rather than advancing the clip a second time.
   *
   * @return world Y of the sprite's current top, or null for a zombie with no rig to measure
   */
  private Float zombieTopY(Zombie zombie) {
    EntityAnimation animation = zombieAnimation(zombie);
    if (animation == null) {
      return null;
    }
    String clip = zombieClip(animation, zombie);
    String anchor = zombieAnchorClip(animation, zombie);
    float time = playback.peek(zombie);
    float flight = zombie.flightProgress();
    double column = flight > 0f
        ? zombie.getX() + (zombie.getThrownFromX() - zombie.getX()) * flight
        : zombie.getX();
    float x = geometry.columnCentreX(onBoard(column));
    float y = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + throwLift(flight);
    float scale = zombieAnimationScale(zombie, animation, anchor);
    float[] top = animation.topPartBox(clip, anchor, time, x, y, scale, zombie.isHypnotized());
    return top == null ? null : top[1] + top[3] / 2f;
  }

  /** Foot Y plus the whole-clip estimate, for a zombie zombieTopY could not measure. */
  private float legacySpriteTopY(Zombie zombie) {
    return geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + zombieSpriteHeight(zombie);
  }

  /** The old, whole-clip estimate: still what a zombie with no rig (a static portrait) needs. */
  private float zombieSpriteHeight(Zombie zombie) {
    EntityAnimation animation = zombieAnimation(zombie);
    if (animation != null) {
      String clip = zombieClip(animation, zombie);
      return animation.height(clip)
          * zombieAnimationScale(zombie, animation, zombieAnchorClip(animation, zombie));
    }
    TextureRegion art = zombieArt.find(zombie.getName());
    return art == null
        ? geometry.getCellHeight() * 0.6f
        : art.getRegionHeight() * zombieScale(zombie, art);
  }

  private static boolean isBoss(Zombie zombie) {
    return zombie.isBoss();
  }

  private static int footRow(Zombie zombie) {
    return zombie.getBottomRow();
  }

  private float zombieScale(Zombie zombie, TextureRegion art) {
    if (isBoss(zombie)) {
      return geometry.getCellHeight() * ZOMBOSS_ROW_FILL / art.getRegionHeight();
    }
    return zombieScaleFrom(art.getRegionWidth(), art.getRegionHeight(),
        referenceHeightFor(zombie));
  }

  private static float referenceHeightFor(Zombie zombie) {
    return ZombieArt.zombotanyPlant(zombie.getName()) != null
        ? ZOMBOTANY_REFERENCE_HEIGHT
        : ZOMBIE_REFERENCE_HEIGHT;
  }

  private float zombieScaleFrom(float spriteWidth, float spriteHeight, float referenceHeight) {
    if (spriteWidth <= 0f || spriteHeight <= 0f || referenceHeight <= 0f) {
      return 0f;
    }
    float scale = geometry.getCellHeight() * ZOMBIE_ROW_FILL / referenceHeight;
    float widest = geometry.getCellHeight() * ZOMBIE_WIDTH_LIMIT_LANES;
    if (spriteWidth * scale > widest) {
      scale = widest / spriteWidth;
    }
    float tallest = geometry.getCellHeight() * ZOMBIE_HEIGHT_LIMIT_LANES;
    if (spriteHeight * scale > tallest) {
      scale = tallest / spriteHeight;
    }
    return scale;
  }

  private float scaleFor(TextureRegion region, float referenceHeight, float rowFill) {
    return scaleFor(region.getRegionWidth(), referenceHeight, rowFill);
  }

  private float scaleFor(float spriteWidth, float referenceHeight, float rowFill) {
    float scale = geometry.getCellHeight() * rowFill / referenceHeight;
    float widest = geometry.getCellHeight() * PLANT_WIDTH_LIMIT_LANES;
    if (spriteWidth * scale > widest) {
      scale = widest / spriteWidth;
    }
    return scale;
  }

  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset) {
    drawStanding(context, region, col, row, scale, footInset, 0f);
  }

  private void drawStanding(RenderContext context, TextureRegion region, double col, int row,
      float scale, float footInset, float angle) {
    float width = region.getRegionWidth() * scale;
    float height = region.getRegionHeight() * scale;
    float bottom = geometry.rowToY(row) + geometry.getCellHeight() * footInset;
    context.getBatch().draw(region, geometry.columnCentreX(col) - width / 2f, bottom,
        width / 2f, height / 2f, width, height, 1f, 1f, angle);
  }

  private float spinAngle(Zombie zombie) {
    return zombie.getBehavior() instanceof JesterZombieAction jester && jester.isSpinning()
        ? (clock * 540f) % 360f
        : 0f;
  }

  /**
   * The King never moves and never eats; what he does is speed up everything in his lane. That is
   * invisible unless the reach is drawn, so his own knighting burst marks it out.
   */
  /**
   * The plant food waiting on the lawn to be picked up. It bobs so it reads as a thing to click
   * rather than scenery, and fades over its last couple of seconds as a warning it is about to go.
   */
  private void drawPlantFoodDrops(RenderContext context, Board board) {
    TextureRegion art = hudArt.find(PLANT_FOOD_REGION);
    if (art == null) {
      return;
    }
    float lane = geometry.getCellHeight();
    for (PlantFoodDrop drop : board.getPlantFoodDrops()) {
      if (drop.isGone()) {
        continue;
      }
      float fade = Math.min(1f, drop.getTicksLeft() / (float) PLANT_FOOD_FADE_TICKS);
      float bob = lane * PLANT_FOOD_BOB_LANES
          * (float) Math.sin(clock * 3.4f + drop.getColumn());
      context.getBatch().setColor(1f, 1f, 1f, fade);
      drawCentred(context, art, drop.getColumn(), drop.getRow(), lane * PLANT_FOOD_DROP_LANES,
          bob, 0f);
    }
    context.getBatch().setColor(Color.WHITE);
  }

  /**
   * The green burst behind a plant working off a dose of plant food. Same aura treatment the King
   * and Ra already get, drawn before the plant so it sits behind it and never covers the artwork,
   * and it is on screen for exactly as long as the dose is active.
   */
  private void drawPlantFoodGlow(RenderContext context, Plant plant) {
    if (!plant.isPlantFoodActive()) {
      return;
    }
    TextureRegion glow = hudArt.find(PLANT_FOOD_GLOW_REGION);
    if (glow == null) {
      return;
    }
    float size = geometry.getCellHeight() * PLANT_FOOD_GLOW_LANES;
    float pulse = 0.5f + 0.28f * (float) Math.sin(clock * PLANT_FOOD_GLOW_SPEED);
    Batch batch = context.getBatch();
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    batch.setColor(plantFoodGlow.r, plantFoodGlow.g, plantFoodGlow.b, pulse);
    batch.draw(glow, geometry.columnCentreX(plant.getCol()) - size / 2f,
        geometry.rowCentreY(plant.getRow()) - size / 2f, size, size);
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    batch.setColor(Color.WHITE);
  }

  private void drawKingAura(RenderContext context, Zombie zombie) {
    TextureRegion aura = hudArt.find("kingaura");
    if (aura == null || !(zombie.getBehavior() instanceof KingAuraZombieAction)) {
      return;
    }
    float size = geometry.getCellHeight() * 2.9f;
    float pulse = 0.32f + 0.12f * (float) Math.sin(clock * 2.5f);
    context.getBatch().setColor(1f, 0.9f, 0.45f, pulse);
    context.getBatch().draw(aura, geometry.columnCentreX(drawColumn(zombie)) - size / 2f,
        geometry.rowCentreY(zombie.getRow()) - size / 2f, size, size);
    context.getBatch().setColor(Color.WHITE);
  }

  /**
   * Ra never eats a plant near it as often as it heals its neighbours, and that healing is
   * otherwise invisible -- a wounded zombie next to him just stops looking wounded with no cue why.
   * Same treatment as {@link #drawKingAura}, in a healing green rather than the King's gold so the
   * two auras are not mistaken for each other.
   */
  private void drawHealAura(RenderContext context, Zombie zombie) {
    TextureRegion aura = hudArt.find("kingaura");
    if (aura == null || !(zombie.getBehavior() instanceof RaHealAuraZombieAction)) {
      return;
    }
    float size = geometry.getCellHeight() * 2.4f;
    float pulse = 0.28f + 0.12f * (float) Math.sin(clock * 2.5f);
    context.getBatch().setColor(0.5f, 0.95f, 0.55f, pulse);
    context.getBatch().draw(aura, geometry.columnCentreX(drawColumn(zombie)) - size / 2f,
        geometry.rowCentreY(zombie.getRow()) - size / 2f, size, size);
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawProjectiles(RenderContext context, Board board) {
    for (Projectile projectile : board.getProjectiles()) {
      ProjectileArt.Shot shot = projectileArt.find(projectile);
      if (shot == null) {
        continue;
      }
      float[] muzzle = muzzleOf(board, projectile);
      double column = shotColumn(projectile, muzzle);
      double row = shotRow(projectile);
      float lift = arcLift(projectile, column) + muzzleLift(projectile, muzzle)
          + (float) projectile.getMuzzleOffset() * geometry.getCellHeight();
      context.getBatch().setColor(projectile.isFromZombie() ? reflectedPea : Color.WHITE);
      drawCentred(context, shot.region(), column, row,
          geometry.getCellHeight() * shot.rowFraction(), lift, shot.angle());
    }
    context.getBatch().setColor(Color.WHITE);
  }

  /**
   * Where to draw a shot this frame.
   *
   * <p>Two things happen here. The shot is drawn between its last tick position and its current
   * one so it glides instead of hopping half a tile at a time, and on the tick it is fired it
   * waits in the plant's muzzle until the shooting animation reaches its release frame before
   * setting off. Neither touches the simulation: the shot has already been placed, has already
   * been tested against zombies and does its damage on exactly the tick it always did.
   */
  private double shotColumn(Projectile projectile, float[] muzzle) {
    double from = projectile.getPreviousX();
    double to = projectile.getXCoordinate();
    if (muzzle == null || from != projectile.getLaunchX()) {
      return lerp(from, to, tickAlpha);
    }
    double mouth = from + muzzle[0];
    float release = muzzle[2];
    if (tickAlpha <= release) {
      return mouth;
    }
    return lerp(mouth, to, (tickAlpha - release) / (1f - release));
  }

  /**
   * The lane to draw a shot in, crossing out of its shooter's lane on the tick it was fired.
   *
   * <p>Threepeater puts a pea in the lane above and the lane below, and the model creates each one
   * already sitting in its lane -- so all the player saw was three peas appearing in three lanes
   * at once, with nothing to say the middle plant had fired them. Nothing about the simulation
   * changes: the shot is in its own lane for every tick of hit testing, and this only decides
   * where it is drawn during the single tick it leaves the plant.
   */
  private double shotRow(Projectile projectile) {
    double travelling = lerp(projectile.getPreviousY(), projectile.getExactY(), tickAlpha);
    int from = projectile.getLaunchRow();
    if (from < 0 || projectile.getPreviousX() != projectile.getLaunchX()) {
      return travelling;
    }
    return lerp(from, travelling, tickAlpha);
  }

  /**
   * The lift that puts a shot in the plant's mouth as it leaves, and nothing once it has left.
   *
   * <p>This used to be added for the shot's whole flight, which meant every pea was drawn at
   * whatever height its shooter's head happened to be at that frame -- so the pea rose and fell in
   * mid-air in time with the plant breathing. Worst on Fire Peashooter, whose head travels
   * furthest. Now it decays across the launch tick, so the shot leaves the mouth and settles onto
   * its lane instead of being tied to the rig for the rest of its life.
   */
  private float muzzleLift(Projectile projectile, float[] muzzle) {
    if (muzzle == null || projectile.getPreviousX() != projectile.getLaunchX()) {
      return 0f;
    }
    float release = muzzle[2];
    float left = tickAlpha <= release ? 0f : (tickAlpha - release) / (1f - release);
    return muzzle[1] * (1f - left);
  }

  private static double lerp(double from, double to, float alpha) {
    return from + (to - from) * alpha;
  }

  /**
   * The mouth of the plant that fired this shot as {forward tiles, lift in pixels, release}, or
   * null when there is nothing to line the shot up with -- a zombie's shot, a lob, or a plant that
   * has since been eaten. The offsets are read off the rig that is on screen rather than being
   * guessed per plant, so nothing about the artwork has to change.
   */
  private float[] muzzleOf(Board board, Projectile projectile) {
    if (projectile.isLobbed()) {
      return null;
    }
    if (projectile.isFromZombie()) {
      Zombie shooter = board.getZombieAt(projectile.getYCoordinate(), projectile.getLaunchX());
      return shooter == null ? null : zombieMuzzles.get(shooter);
    }
    int col = (int) Math.round(projectile.getLaunchX());
    Plant plant = board.getPlantAt(projectile.getYCoordinate(), col);
    if (plant == null || plant.isDead()) {
      return null;
    }
    return muzzles.get(plant);
  }

  private float arcLift(Projectile projectile, double column) {
    if (!projectile.isLobbed()) {
      return 0f;
    }
    double span = projectile.getTargetX() - projectile.getLaunchX();
    if (span <= 0) {
      return 0f;
    }
    double travelled = (column - projectile.getLaunchX()) / span;
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
        geometry.rowCentreY(row) - targetHeight / 2f + lift,
        width / 2f, targetHeight / 2f, width, targetHeight, 1f, 1f, angle);
  }

  /**
   * Where a falling hazard is going to land, drawn on the ground and closing as it comes down.
   *
   * <p>Without it the player has no way to move out of the way, which is the difference between an
   * attack that can be played around and one that simply happens to them.
   */
  private void drawHazardTargets(ShapeRenderer shapes, Board board) {
    float lane = geometry.getCellHeight();
    for (BossHazard hazard : board.getBossHazards()) {
      if (!hazard.isFalling()) {
        continue;
      }
      float closing = 1f - (float) hazard.fallFraction();
      shapes.setColor(hazardTarget.r, hazardTarget.g, hazardTarget.b, 0.25f + 0.5f * closing);
      float radius = lane * (0.44f - 0.2f * closing);
      shapes.circle(geometry.columnCentreX(hazard.getColumn()),
          geometry.rowCentreY(hazard.getRow()), radius, 24);
    }
  }

  private void drawShapes(RenderContext context, Board board) {
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);

    shapes.setColor(peaColor);
    for (Projectile projectile : board.getProjectiles()) {
      if (projectileArt.find(projectile) == null) {
        shapes.circle(
            geometry.columnCentreX(
                lerp(projectile.getPreviousX(), projectile.getXCoordinate(), tickAlpha)),
            geometry.rowCentreY(
                lerp(projectile.getPreviousY(), projectile.getExactY(), tickAlpha)), 7f);
      }
    }
    if (hudArt.find("sun") == null) {
      shapes.setColor(sunColor);
      for (Sun sun : board.getSuns()) {
        shapes.circle(geometry.columnCentreX(sun.getX()),
            geometry.rowCentreY(lerp(sun.getPreviousY(), sun.getY(), tickAlpha)), 16f);
      }
    }
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead()) {
        healthBar(shapes, zombie);
      }
    }
    drawHazardTargets(shapes, board);
    drawHitBursts(shapes);
    drawDeathPuffs(shapes);
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    drawMissingArtOutlines(shapes, board);
    shapes.end();
  }

  /** A plain box for anything the renderer has neither a rig nor a still for. */
  private void drawMissingArtOutlines(ShapeRenderer shapes, Board board) {
    shapes.setColor(noArt);
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombieArt.find(zombie.getName()) == null
          && zombieAnimation(zombie) == null) {
        shapes.rect(geometry.columnCentreX(drawColumn(zombie)) - geometry.getCellWidth() * 0.25f,
            geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * 0.1f,
            geometry.getCellWidth() * 0.5f, geometry.getCellHeight() * 0.7f);
      }
    }
    for (Plant plant : board.getPlants()) {
      if (plant.isDead() || hasPlantArt(plant)) {
        continue;
      }
      shapes.rect(geometry.columnCentreX(plant.getCol()) - geometry.getCellWidth() * 0.22f,
          geometry.rowToY(plant.getRow()) + geometry.getCellHeight() * 0.16f,
          geometry.getCellWidth() * 0.44f, geometry.getCellHeight() * 0.56f);
    }
  }

  private void drawHitBursts(ShapeRenderer shapes) {
    if (hudArt.find(SPLAT_REGION) != null) {
      return;
    }
    for (HitEffects.Burst burst : hits.getBursts()) {
      burstColor.a = burst.alpha() * 0.75f;
      shapes.setColor(burstColor);
      shapes.circle(geometry.columnCentreX(burst.column()),
          geometry.rowCentreY(burst.row()),
          geometry.getCellHeight() * burst.radiusFraction());
    }
    burstColor.a = 1f;
  }

  private static final float[] DEATH_PUFF_OFFSETS_X = {-0.14f, 0.16f, 0f};
  private static final float[] DEATH_PUFF_OFFSETS_Y = {0.08f, 0.05f, 0.22f};

  private void drawDeathPuffs(ShapeRenderer shapes) {
    if (hudArt.find(DUST_REGION) != null) {
      return;
    }
    for (HitEffects.DeathPuff puff : hits.getDeathPuffs()) {
      dustColor.a = puff.alpha() * 0.7f;
      shapes.setColor(dustColor);
      float cx = geometry.columnCentreX(onBoard(puff.column()));
      float cy = geometry.rowCentreY(puff.row());
      float radius = geometry.getCellHeight() * (0.14f + 0.2f * puff.progress());
      for (int i = 0; i < DEATH_PUFF_OFFSETS_X.length; i++) {
        shapes.circle(cx + geometry.getCellWidth() * DEATH_PUFF_OFFSETS_X[i],
            cy + geometry.getCellHeight() * (DEATH_PUFF_OFFSETS_Y[i] + 0.3f * puff.progress()),
            radius);
      }
    }
    dustColor.a = 1f;
  }

  private boolean hasPlantArt(Plant plant) {
    if (plant.isCursed() && hudArt.find("sheep") != null) {
      return true;
    }
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plant.getName());
    return (animation != null && plantClip(animation, plant, false) != null)
        || plantArt.find(plant.getName()) != null;
  }

  private static final float HEALTH_BAR_GAP = 10f;

  private void healthBar(ShapeRenderer shapes, Zombie zombie) {
    Float topY = zombieTopY(zombie);
    float y = (topY != null ? topY : legacySpriteTopY(zombie)) + HEALTH_BAR_GAP;
    float width = geometry.getCellHeight() * (isBoss(zombie) ? 2.1f : 0.62f);
    float flight = zombie.flightProgress();
    double column = flight > 0f
        ? onBoard(zombie.getX() + (zombie.getThrownFromX() - zombie.getX()) * flight)
        : drawColumn(zombie);
    float x = geometry.columnCentreX(column) - width / 2f;
    // a tall zombie in the top lane would otherwise wear its bar up in the seed cards
    y = Math.min(y, geometry.rowToY(0) + geometry.getCellHeight() - 7f);
    float thickness = isBoss(zombie) ? 11f : 5f;

    float health = zombie.getCurrentHealth() / (float) Math.max(1, zombie.getMaxHealth());
    bar(shapes, x, y, width, thickness, health,
        health < 0.35f ? healthLow : healthFront);

    int armourMax = zombie.getMaxArmorHealth();
    if (armourMax > 0 && zombie.hasIntactArmor()) {
      float armourThickness = Math.max(3f, thickness - 2f);
      bar(shapes, x, y + thickness + 2f, width, armourThickness,
          zombie.getRemainingArmorHealth() / (float) armourMax, armourTint);
    }
  }

  private void bar(ShapeRenderer shapes, float x, float y, float width, float thickness,
      float fraction, Color fill) {
    float clamped = Math.max(0f, Math.min(1f, fraction));
    shapes.setColor(healthBack);
    shapes.rect(x - 1f, y - 1f, width + 2f, thickness + 2f);
    shapes.setColor(fill);
    shapes.rect(x, y, width * clamped, thickness);
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
    projectileArt.dispose();
    currencyArt.dispose();
    animations.dispose();
  }
}
