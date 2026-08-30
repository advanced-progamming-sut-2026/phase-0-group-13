package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.core.GameManager;
import model.enums.StatusEffect;
import model.enums.SunType;
import model.game.Board;
import model.game.LootDropper;
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
import view.gdx.core.GdxConfig;
import view.gdx.ui.CurrencyArt;
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

  /**
   * How far a zombie may spill sideways, measured in lanes rather than in tiles.
   *
   * <p>Wider than a tile on purpose: a gargantuar overlapping the columns either side of it is how
   * the game reads, and clamping to one tile is what was flattening the roster.
   *
   * <p>In lanes, not in tiles, because a lane is the one dimension the art is scaled against. A
   * PvZ2 tile is taller than it is wide (see {@link SeasonBackdrop}), so a limit written in tile
   * widths would silently resize every rig in the game the moment the board's proportions were
   * corrected -- which is a rendering change dressed up as a roster change.
   */
  private static final float ZOMBIE_WIDTH_LIMIT_LANES = 1.62f;
  /** And how tall, in lanes -- past this it starts covering the lanes above and below. */
  private static final float ZOMBIE_HEIGHT_LIMIT_LANES = 1.7f;
  /** The same for a plant, which is drawn to sit in its tile rather than to straddle two. */
  private static final float PLANT_WIDTH_LIMIT_LANES = 1.06f;
  /** The plant seed-packet set the four Zombotany zombies borrow from is drawn much smaller. */
  private static final float ZOMBOTANY_REFERENCE_HEIGHT = 62f;

  /** Columns from the house at which a zombie starts showing the warning tint. */
  private static final double NEAR_HOUSE_COLUMN = 1.2;
  private static final float PLANT_FOOT_INSET = 0.14f;

  /**
   * The soft ellipse the greenhouse already puts under a pot, reused to ground everything on the
   * lawn. Without it plants and zombies read as cut-outs pasted on the tiles; PVZ2 sits every
   * entity on one of these. Kept faint on purpose -- it is depth, not decoration, and the lane
   * has to stay readable.
   *
   * <p>Sized in lanes for the same reason the sprite limits are (see
   * {@link #ZOMBIE_WIDTH_LIMIT_LANES}): a shadow has to keep its proportion to the thing standing
   * on it, not to the width of the tile underneath.
   */
  private static final String SHADOW_REGION = "potshadow";
  private static final float SHADOW_ALPHA = 0.36f;
  private static final float PLANT_SHADOW_WIDTH_LANES = 0.74f;
  private static final float ZOMBIE_SHADOW_WIDTH_LANES = 0.66f;
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
  // Fallback hold for a rig whose attack clip the manifest gives no duration for. Normally the
  // clip's own length is used instead; see justActed.
  private static final int PLANT_ATTACK_HOLD_TICKS = 4;
  // How far above its lane a lobbed shot rises at the top of the arc.
  private static final float LOB_ARC_HEIGHT = 0.85f;
  private static final float FREEZE_LEVELS = Plant.MAX_FREEZE_LEVEL;
  // The octopus a beach thrower leaves on a plant. It is one prop out of that zombie's own rig --
  // the little orange one with the eyes, checked against the atlas page before being named here.
  private static final String OCTOPUS_RIG = "zombiebeachoctopus";
  private static final String OCTOPUS_REGION = "zombie_beach_octopus_66x76";
  private static final float OCTOPUS_ROW_FILL = 0.44f;

  /**
   * The in-match feedback art, all of it out of the same HUD sheet the sun and the pea come from.
   *
   * <p>Every one of these events used to be either a ShapeRenderer ring or nothing at all. They
   * are the original game's own pieces for the same moments: the pea splat, the explosion cloud
   * and the ash a zombie leaves, the white star a mower throws, the soft one plant food leaves on
   * the ground, a spray of dirt, and the gold burst armour comes apart in.
   */
  private static final String SPLAT_REGION = "splatpea";
  private static final String DUST_REGION = "dustpuff";
  private static final String ASH_REGION = "zombieash";
  private static final String DIRT_REGION = "dirtclods";
  private static final String PLANT_PUFF_REGION = "plantpuff";
  private static final String ARMOUR_BREAK_REGION = "armourbreak";
  /** Spark kinds, matched in {@link #sparkArt}. */
  static final String SPARK_ARMOUR = "armour";
  static final String SPARK_PLANTED = "planted";
  static final String SPARK_SUN = "sun";
  /** Impact sprite size at the end of its life, in lanes. It starts at half this and grows. */
  private static final float SPLAT_SIZE_LANES = 0.42f;
  private static final float DUST_SIZE_LANES = 0.95f;
  private static final float ASH_SIZE_LANES = 0.5f;
  private static final float SPARK_SIZE_LANES = 0.8f;

  /** How big a lawn pickup icon is drawn, as a fraction of a cell's height. */
  private static final float LOOT_ICON_FRACTION = 0.34f;
  /** How far a pickup floats upward over its life, in cells. */
  private static final float LOOT_LIFT_FRACTION = 0.7f;

  /** Camera jolt added per zombie death; several dying at once stacks up to MAX_SHAKE. */
  private static final float SHAKE_PER_DEATH = 3.4f;
  private static final float MAX_SHAKE = 9f;
  private static final float SHAKE_DECAY_PER_SECOND = 22f;
  // Clamped over the plant's head rather than centred on the tile, and off to one side so it does
  // not simply cover the face of whatever it caught.
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
  private final Color dustColor = new Color(0.4f, 0.35f, 0.28f, 1f);
  // King's aura pulse and the Juggler's spin both read this; it only ticks in render().
  private float clock;
  // How hard the world camera is currently kicking from recent zombie deaths; decays to 0.
  private float shakeMagnitude;
  private float shakeSeed;
  // The match's own tick, for lining a plant's attack clip up with the shot it just fired.
  private int currentTick;
  private TextureRegion octopus;
  private boolean octopusChecked;
  /** Plants and suns seen last frame, so an arrival or a departure can be spotted. */
  private final Map<Plant, Boolean> knownPlants = new java.util.IdentityHashMap<>();
  private final Map<Plant, Boolean> seenPlants = new java.util.IdentityHashMap<>();
  private final Map<Sun, double[]> knownSuns = new java.util.IdentityHashMap<>();
  private final Map<Sun, double[]> seenSuns = new java.util.IdentityHashMap<>();
  private boolean seenABoard;

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
    spawnLootPickups(board);
    observeForEffects(board);
    applyShake(context, delta, hits.drainFreshDeaths());
    drawSprites(context, board, delta);
    drawShapes(context, board);
    hits.endFrame(geometry.getColumns());
    playback.endFrame();
  }

  /**
   * Every entity's ground shadow, in one pass before anything stands on them.
   *
   * <p>Its own pass rather than one shadow per sprite, so a shadow can never land on top of the
   * neighbour drawn before it. Costs no extra texture: HudArt is already loaded for the sun and
   * the pea.
   */
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
        drawShadow(context, shadow, onBoard(zombie.getX()), zombie.getRow(),
            ZOMBIE_SHADOW_WIDTH_LANES, ZOMBIE_FOOT_INSET);
      }
    }
    context.getBatch().setColor(Color.WHITE);
  }

  /** One shadow, centred on the foot line the sprite above it stands on. */
  private void drawShadow(RenderContext context, TextureRegion shadow, double column, int row,
      float widthInLanes, float footInset) {
    float width = geometry.getCellHeight() * widthInLanes;
    float height = width * shadow.getRegionHeight() / (float) shadow.getRegionWidth();
    context.getBatch().draw(shadow,
        geometry.columnCentreX(column) - width / 2f,
        geometry.rowToY(row) + geometry.getCellHeight() * footInset - height / 2f,
        width, height);
  }

  /** Turns whatever LootDropper queued this frame into pickups sitting on the lawn. */
  private void spawnLootPickups(Board board) {
    for (LootDropper.LootSpawn spawn : board.drainPendingLootSpawns()) {
      hits.spawnPickup(spawn.kind(), spawn.column(), spawn.row());
    }
  }

  /**
   * Kicks the world camera for every zombie that just died, then decays it back to rest.
   *
   * <p>Always recomputed from the camera's own centre rather than a remembered "rest" position, so
   * a screen that gets disposed mid-shake never leaves the shared camera stuck off-centre for
   * whatever renders through it next.
   */
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
    context.getBatch().begin();
    drawGroundShadows(context, board);
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
    context.getBatch().setColor(Color.WHITE);
    drawImpacts(context);
    drawDeaths(context);
    drawSparks(context);
    context.getBatch().setColor(Color.WHITE);
    context.getBatch().end();
  }

  /**
   * The splat where a shot landed.
   *
   * <p>Grows from half size and fades, which is what makes a splat read as an impact rather than
   * as a sticker: the eye catches the change, not the shape. Drawn after the entities so a hit on
   * a zombie's face is not hidden behind the zombie.
   */
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

  /**
   * Where a zombie went down: the explosion cloud swelling and thinning, with the ash under it.
   *
   * <p>Two pieces because the original plays it as two: the cloud carries the motion and the ash
   * is what is left on the tile, so the ash barely grows and fades later than the cloud does.
   */
  private void drawDeaths(RenderContext context) {
    TextureRegion cloud = hudArt.find(DUST_REGION);
    if (cloud == null) {
      return;
    }
    TextureRegion ash = hudArt.find(ASH_REGION);
    for (HitEffects.DeathPuff puff : hits.getDeathPuffs()) {
      float lane = geometry.getCellHeight();
      double column = onBoard(puff.column());
      if (ash != null) {
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

  /**
   * The one-off bursts.
   *
   * <p>Each kind gets its own art, size and height off the tile, because they mean different
   * things: armour comes apart around the zombie wearing it, soil is thrown at ground level where
   * the plant went in, and a collected sun pops where it was picked up.
   */
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
          // At the plant's foot, not across its middle: drawCentred measures from the middle of
          // the lane, so getting the soil onto the ground means coming back down most of a half
          // lane. Kept low and wide -- it is meant to read as the tile being disturbed, not as a
          // smear over the plant that just went in.
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

  /** The icon for one kind of lawn drop, or null if nothing to draw it with is loaded. */
  private TextureRegion lootIcon(String kind) {
    return switch (kind) {
      case "coin" -> currencyArt.findCoin();
      case "diamond" -> currencyArt.findGem();
      case "pot" -> hudArt.find("pot");
      default -> null;
    };
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
        seenPlants.put(plant, Boolean.TRUE);
        noteNewPlant(plant);
      }
    }
    // Rebuilt from the board every frame, so a dug-up plant's entry goes with it.
    knownPlants.clear();
    knownPlants.putAll(seenPlants);
    seenPlants.clear();
    hits.forgetCounts(entity -> entity instanceof Zombie zombie && zombie.isDead());
    seenABoard = true;
    for (Zombie zombie : board.getZombies()) {
      hits.observeZombieState(zombie, zombie.isDead(), zombie.getX(), zombie.getRow());
      if (!zombie.isDead()) {
        hits.observe(zombie, zombie.getCurrentHealth());
        // Armour is a count that goes down. Nothing announces a piece breaking, so this watches
        // for it: the piece simply stopped being drawn, which on its own reads as a glitch.
        hits.observeCount(zombie, intactArmour(zombie), SPARK_ARMOUR,
            onBoard(zombie.getX()), zombie.getRow());
      }
    }
    for (Projectile projectile : board.getProjectiles()) {
      hits.observeProjectile(projectile, projectile.getXCoordinate(),
          Math.round(projectile.getYCoordinate()));
    }
    noteCollectedSuns(board);
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

  /**
   * A puff of soil for a plant that was not on the board last frame.
   *
   * <p>Skipped on the first frame a board is seen, so a stage that starts with plants already
   * standing (Save Our Seeds) does not open with a shower of dirt.
   */
  private void noteNewPlant(Plant plant) {
    if (!knownPlants.containsKey(plant) && seenABoard) {
      hits.spawnSpark(SPARK_PLANTED, plant.getCol(), plant.getRow());
    }
  }

  /**
   * A pop where a sun left the lawn.
   *
   * <p>Collecting sun is the one thing the player does constantly, and the sun simply vanished:
   * with the counter up in the corner there was nothing at the pointer to say the click landed.
   * Suns that time out get the same pop, which is honest -- both are "that sun is gone now".
   */
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
    String attack = animation.pickClip("attack");
    if (attack != null && justActed(plant, animation.duration(attack))) {
      return attack;
    }
    return animation.pickClip("idle", "attack");
  }

  /**
   * True while the plant's attack clip is still running.
   *
   * <p>Held for as long as the clip itself lasts rather than for a fixed number of ticks. Every
   * attack used to be cut off after {@link #PLANT_ATTACK_HOLD_TICKS} and snapped back to idle
   * part-way through the motion: a Peashooter got 39% of its second-long shot, a Cabbage-pult 24%
   * of its throw, and a Repeater -- whose clip is one volley of two peas -- only ever played the
   * first of them. Reading the length off the rig fixes all of them at once and needs no per-plant
   * numbers.
   *
   * <p>Not capped. The clips run from a third of a second (Bonk Choy's punch) to four and a half
   * (Hot Potato thawing), and each of those is how long that plant is genuinely busy, so the rig's
   * own number is the answer in both directions. A plant whose attack outlasts the gap between its
   * shots simply stays in attack, which is what it is in fact doing. The fallback is the old fixed
   * hold, for a clip the manifest gives no duration for.
   */
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
    return zombieScaleFrom(animation.width(clip), height, ZOMBIE_ANIM_UNITS);
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
    if (isBoss(zombie)) {
      return geometry.getCellHeight() * ZOMBOSS_ROW_FILL / art.getRegionHeight();
    }
    return zombieScaleFrom(art.getRegionWidth(), art.getRegionHeight(),
        referenceHeightFor(zombie));
  }

  /**
   * Which reference a portrait is measured against.
   *
   * <p>The zombie packet page is one coherent set drawn to a common scale, so measuring all of it
   * against one reference is what keeps an imp smaller than a gargantuar. The four Zombotany
   * zombies are the exception: they have no art of their own and borrow a *plant* seed packet,
   * which is a different set drawn much smaller, so against the zombie reference they came out at
   * a bit over half a lane -- smaller than an imp, for a zombie that is meant to read as an
   * ordinary walker.
   */
  private static float referenceHeightFor(Zombie zombie) {
    return ZombieArt.zombotanyPlant(zombie.getName()) != null
        ? ZOMBOTANY_REFERENCE_HEIGHT
        : ZOMBIE_REFERENCE_HEIGHT;
  }

  /**
   * How big to draw a zombie, given its own art size and the reference its set is drawn to.
   *
   * <p>Deliberately not "every zombie the same size": the ratio of a sprite to its set's reference
   * is the relative size the artists drew, and flattening it would make a gargantuar the size of
   * an imp. The two limits only stop the extremes from breaking the board -- a zombie may be wider
   * than its tile, the way a gargantuar is, but it may not be so tall that it covers the lanes
   * either side of the one it is walking in.
   *
   * <p>The width limit used to be 0.95 of a tile, which is right for a plant -- a plant occupies
   * exactly one tile -- but wrong for a zombie: it fired on almost every rig in the game and
   * rescaled it by its aspect ratio instead of its size. That is what made the Egypt Gargantuar,
   * the largest rig in the roster at 441x519 units, draw barely taller than a mummy, and the wide,
   * squat Barrel Roller draw smaller than an imp.
   */
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
    // nothing may spill far enough sideways to sit over its neighbour's tile
    float widest = geometry.getCellHeight() * PLANT_WIDTH_LIMIT_LANES;
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
    float size = geometry.getCellHeight() * 2.9f;
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
    drawDeathPuffs(shapes);
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
   * The small burst where a projectile landed, for as long as there is no art for it.
   *
   * <p>The library does have the art -- see {@link #drawImpacts} -- so this only runs on a build
   * whose HUD sheet predates it, and a ring that grows and fades still reads as a hit.
   */
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

  /** Fixed offsets for the three dust puffs of one death, so they scatter without needing a Random. */
  private static final float[] DEATH_PUFF_OFFSETS_X = {-0.14f, 0.16f, 0f};
  private static final float[] DEATH_PUFF_OFFSETS_Y = {0.08f, 0.05f, 0.22f};

  /**
   * Where a zombie died, for as long as there is no art for it. See {@link #drawDeaths}.
   */
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

  /** Whether anything at all is drawn for this plant: its own rig, or failing that its packet. */
  private boolean hasPlantArt(Plant plant) {
    if (plant.isCursed() && hudArt.find("sheep") != null) {
      return true;
    }
    EntityAnimation animation = animations.find(AnimationLibrary.PLANTS, plant.getName());
    return (animation != null && plantClip(animation, plant) != null)
        || plantArt.find(plant.getName()) != null;
  }

  /**
   * Sits just above the sprite, so a tall zombie does not wear its bar on its chest.
   *
   * <p>An armoured zombie gets a second bar stacked over the first for what its cone, bucket or
   * helmet has left. They are separate pools in the model -- {@link Zombie#takeDamage} spends the
   * armour before the body and a piercing hit skips it -- so one blended bar would say a zombie was
   * nearly dead while its bucket was still taking every shot. Once the armour is gone the strip
   * goes with it, which is the same moment the rig stops drawing the headwear.
   */
  private void healthBar(ShapeRenderer shapes, Zombie zombie) {
    float spriteHeight = zombieSpriteHeight(zombie);
    // the chapter ends when this one dies, so its bar has to be readable from across the lawn
    float width = geometry.getCellHeight() * (isBoss(zombie) ? 2.1f : 0.62f);
    float x = geometry.columnCentreX(onBoard(zombie.getX())) - width / 2f;
    float y = geometry.rowToY(zombie.getRow()) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + spriteHeight + 4f;
    // a tall zombie in the top lane would otherwise wear its bar up in the seed cards
    y = Math.min(y, geometry.rowToY(0) + geometry.getCellHeight() - 7f);
    float thickness = isBoss(zombie) ? 11f : 5f;

    float health = zombie.getCurrentHealth() / (float) Math.max(1, zombie.getMaxHealth());
    bar(shapes, x, y, width, thickness, health,
        health < 0.35f ? healthLow : healthFront);

    int armourMax = zombie.getMaxArmorHealth();
    if (armourMax > 0 && zombie.hasIntactArmor()) {
      // Thinner than the health bar and directly over it: the eye reads the pair as one stack, and
      // it is the health underneath that decides whether the zombie is nearly down.
      float armourThickness = Math.max(3f, thickness - 2f);
      bar(shapes, x, y + thickness + 2f, width, armourThickness,
          zombie.getRemainingArmorHealth() / (float) armourMax, armourTint);
    }
  }

  /** One backed bar: a dark plate, then the fill clamped to 0..1. */
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
