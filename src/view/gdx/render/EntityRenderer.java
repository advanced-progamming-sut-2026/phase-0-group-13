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
import model.game.zombie.behavior.BarrelRollerZombieAction;
import model.game.zombie.behavior.EnrageOnArmorBreakZombieAction;
import model.game.zombie.behavior.GargantuarAction;
import model.game.zombie.behavior.HookPullZombieAction;
import model.game.zombie.behavior.HunterZombieAction;
import model.game.zombie.behavior.JesterZombieAction;
import model.game.zombie.behavior.KingAuraZombieAction;
import model.game.zombie.behavior.OctopusThrowerZombieAction;
import model.game.zombie.behavior.RaHealAuraZombieAction;
import model.game.zombie.behavior.TacklerZombieAction;
import model.game.zombie.behavior.TombRaiserZombieAction;
import model.game.zombie.behavior.TurquoiseZombieAction;
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
  private static final float FREEZE_LEVELS = Plant.MAX_FREEZE_LEVEL;
  private static final String OCTOPUS_RIG = "zombiebeachoctopus";
  private static final String OCTOPUS_REGION = "zombie_beach_octopus_66x76";
  private static final float OCTOPUS_ROW_FILL = 0.44f;

  private static final String SPLAT_REGION = "splatpea";
  private static final String DUST_REGION = "dustpuff";
  private static final String ASH_REGION = "zombieash";
  private static final String DIRT_REGION = "dirtclods";
  private static final String PLANT_PUFF_REGION = "plantpuff";
  private static final String ARMOUR_BREAK_REGION = "armourbreak";
  static final String SPARK_ARMOUR = "armour";
  static final String SPARK_PLANTED = "planted";
  static final String SPARK_SUN = "sun";
  private static final float SPLAT_SIZE_LANES = 0.42f;
  private static final float DUST_SIZE_LANES = 0.95f;
  private static final float ASH_SIZE_LANES = 0.5f;
  private static final float SPARK_SIZE_LANES = 0.8f;

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
  private final Color peaColor = new Color(0.55f, 0.9f, 0.3f, 1f);
  private final Color sunColor = new Color(1f, 0.85f, 0.2f, 1f);
  private final Color noArt = new Color(1f, 1f, 1f, 0.85f);
  private final Color frozenTint = new Color(0.45f, 0.7f, 1f, 1f);
  private final Color chilledTint = new Color(0.72f, 0.88f, 1f, 1f);
  private final Color hypnoTint = new Color(0.85f, 0.6f, 1f, 1f);
  private final Color shieldTint = new Color(0.6f, 0.8f, 0.98f, 1f);
  private final Color icedTint = new Color(0.55f, 0.8f, 1f, 1f);
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
  private TextureRegion octopus;
  private boolean octopusChecked;
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
        drawShadow(context, shadow, onBoard(zombie.getX()), footRow(zombie),
            ZOMBIE_SHADOW_WIDTH_LANES, ZOMBIE_FOOT_INSET);
      }
    }
    context.getBatch().setColor(Color.WHITE);
  }

  private void drawPlant(RenderContext context, Plant plant, TextureRegion sheep, float delta) {
    boolean cursed = plant.isCursed() && sheep != null;
    context.getBatch().setColor(flashed(plantTint(plant), plant));
    if (!cursed && drawPlantAnimation(context, plant, delta)) {
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
    if (drawZombieAnimation(context, zombie, delta)) {
      return;
    }
    TextureRegion art = zombieArt.find(zombie.getName());
    if (art != null) {
      drawStanding(context, art, onBoard(zombie.getX()), footRow(zombie),
          zombieScale(zombie, art), ZOMBIE_FOOT_INSET, spinAngle(zombie));
    }
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
    context.getBatch().begin();
    drawGroundShadows(context, board);
    TextureRegion sheep = hudArt.find("sheep");
    for (int row = 0; row < board.getRows(); row++) {
      for (Plant plant : board.getPlants()) {
        if (plant.getRow() == row) {
          drawPlant(context, plant, sheep, delta);
        }
      }
      // Its own pass over the row, so an octopus is never hidden under the neighbour drawn next.
      context.getBatch().setColor(Color.WHITE);
      for (Plant plant : board.getPlants()) {
        if (plant.getRow() == row) {
          drawOctopusHold(context, plant);
        }
      }
      context.getBatch().setColor(Color.WHITE);
      for (Zombie zombie : board.getZombies()) {
        if (footRow(zombie) == row && !zombie.isDead()) {
          drawZombie(context, zombie, delta);
        }
      }
      context.getBatch().setColor(Color.WHITE);
    }
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
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        hits.observe(plant, plant.getCurrentHealth());
        seenPlants.put(plant, Boolean.TRUE);
        noteNewPlant(plant);
      }
    }
    knownPlants.clear();
    knownPlants.putAll(seenPlants);
    seenPlants.clear();
    hits.forgetCounts(entity -> entity instanceof Zombie zombie && zombie.isDead());
    seenABoard = true;
    for (Zombie zombie : board.getZombies()) {
      hits.observeZombieState(zombie, zombie.isDead(), zombie.getX(), footRow(zombie));
      if (!zombie.isDead()) {
        hits.observe(zombie, zombie.getCurrentHealth());
        hits.observeCount(zombie, intactArmour(zombie), SPARK_ARMOUR,
            onBoard(zombie.getX()), footRow(zombie));
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

  private void noteNewPlant(Plant plant) {
    if (!knownPlants.containsKey(plant) && seenABoard) {
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

  private static final Map<String, String[]> ACTION_CLIP_NAMES = Map.of(
      "Chomper", new String[] {"bite"},
      "Magnet-shroom", new String[] {"catch", "busy"},
      "Squash", new String[] {"jump_down_left", "jump_down_right", "jump_up_left", "jump_up_right"},
      "Fume-shroom", new String[] {"special"},
      "Sun-shroom", new String[] {"special"});

  private String plantClip(EntityAnimation animation, Plant plant) {
    int stage = growthStage(plant);
    String[] names = ACTION_CLIP_NAMES.getOrDefault(plant.getName(), new String[] {"attack"});
    String attack = animation.pickClip(withStage(names, stage));
    if (attack != null && justActed(plant, animation.duration(attack))) {
      return attack;
    }
    String[] idleNames = stage > 0
        ? new String[] {"idle_stage" + stage, "idle"}
        : new String[] {"idle"};
    return animation.pickClip(concat(idleNames, names));
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
    float time = playback.advance(zombie, clip, delta * animationRate(zombie));
    float flight = zombie.flightProgress();
    double column = flight > 0f
        ? zombie.getX() + (zombie.getThrownFromX() - zombie.getX()) * flight
        : zombie.getX();
    float x = geometry.columnCentreX(onBoard(column));
    float y = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + throwLift(flight);
    float scale = zombieAnimationScale(zombie, animation, clip);
    animation.draw(context.getBatch(), clip, time, x, y, scale, zombie.isHypnotized(),
        armourVisibility(animation, zombie));
    drawPlantHead(context, zombie, animation, clip, time, x, y, scale);
    return true;
  }

  private float throwLift(float flight) {
    if (flight <= 0f) {
      return 0f;
    }
    return geometry.getCellHeight() * THROW_ARC_HEIGHT * 4f * flight * (1f - flight);
  }

  private void drawPlantHead(RenderContext context, Zombie zombie, EntityAnimation body,
      String clip, float time, float x, float y, float scale) {
    String plant = ZombieArt.zombotanyPlant(zombie.getName());
    if (plant == null) {
      return;
    }
    EntityAnimation rig = animations.find(AnimationLibrary.PLANTS, plant);
    String idle = rig == null ? null : rig.pickClip("idle");
    float[] head = body.topPartBox(clip, time, x, y, scale, zombie.isHypnotized());
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
        return animation.pickClip("missile_start", "fire_attack", "suction_loop",
            "slingshot", "rocket_launch", "idle");
      default:
        return animation.pickClip("idle");
    }
  }

  private static final int ACTION_POSE_TICKS = 8;

  private String zombieClip(EntityAnimation animation, Zombie zombie) {
    if (zombie.isBoss()) {
      String boss = bossClip(animation, zombie);
      if (boss != null) {
        return boss;
      }
    }
    String suffix = propSuffix(zombie);
    if (zombie.isEating()) {
      String eat = animation.pickClip("eat" + suffix, "eat");
      if (eat != null) {
        return eat;
      }
    }
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
      String throwing = animation.pickClip("smash_left", "fire", "cannon_fire");
      if (throwing != null) {
        return throwing;
      }
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
      names = new String[] {"kick", "tackle"};
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
      context.getBatch().setColor(projectile.isFromZombie() ? reflectedPea : Color.WHITE);
      drawCentred(context, shot.region(), projectile.getXCoordinate(),
          projectile.getYCoordinate(), geometry.getCellHeight() * shot.rowFraction(),
          arcLift(projectile), shot.angle());
    }
    context.getBatch().setColor(Color.WHITE);
  }

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

    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(noArt);
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && zombieArt.find(zombie.getName()) == null
          && zombieAnimation(zombie) == null) {
        shapes.rect(geometry.columnCentreX(onBoard(zombie.getX())) - geometry.getCellWidth() * 0.25f,
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
    shapes.end();
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
    return (animation != null && plantClip(animation, plant) != null)
        || plantArt.find(plant.getName()) != null;
  }

  private void healthBar(ShapeRenderer shapes, Zombie zombie) {
    float spriteHeight = zombieSpriteHeight(zombie);
    float width = geometry.getCellHeight() * (isBoss(zombie) ? 2.1f : 0.62f);
    float x = geometry.columnCentreX(onBoard(zombie.getX())) - width / 2f;
    float y = geometry.rowToY(footRow(zombie)) + geometry.getCellHeight() * ZOMBIE_FOOT_INSET
        + spriteHeight + 4f;
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
