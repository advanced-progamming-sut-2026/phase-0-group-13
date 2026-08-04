package model.game.plant.Factory;

import data.repository.PlantRepository;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.PlantFood;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.*;

public class PlantFactory {
  private static final int TICKS_PER_SECOND = 10;

  private static final int DEFAULT_INTERVAL_SECONDS = 3;
  private static final int DEFAULT_DAMAGE = 20;
  private static final int INSTA_KILL_DAMAGE = 10_000;

  private final PlantRepository repository;

  public PlantFactory(PlantRepository repository) {
    this.repository = repository;
  }

  public Plant createPlant(String name, int row, int col) {
    return createPlant(name, row, col, 1);
  }

  public Plant createPlant(String name, int row, int col, int level) {
    PlantTemplate template = this.repository.find(name.toLowerCase());

    if (template == null) {
      System.err.println("Plant template not found: " + name);
      return null;
    }

    PlantCategory category = determineCategory(template.category);
    EnumSet<PlantTag> tags = parseTags(template.tags);

    double baseIntervalSeconds = parseActionIntervalSeconds(template.actionInterval);
    int baseDamage = parseDamage(template.damage);

    PlantLevel levelStats = PlantLevel.cumulative(template, level);

    double intervalSeconds =
            Math.max(0.1, baseIntervalSeconds + levelStats.getActionIntervalDeltaSeconds());
    int interval = Math.max(1, (int) Math.round(intervalSeconds * TICKS_PER_SECOND));

    int damage = Math.max(0, baseDamage + levelStats.getDamageDelta());
    int hp = Math.max(1, template.baseHp + levelStats.getHpDelta());
    int cost = Math.max(0, template.cost + levelStats.getCostDelta());

    PlantAction behavior = determineBehavior(category, interval, damage, tags, template);
    PlantFood plantFood = determinePlantFood(template, category, interval, damage, tags);

    return new Plant(template, row, col, category, tags, behavior, plantFood, level, hp, cost);
  }

  private PlantCategory determineCategory(String catStr) {
    if (catStr == null) return PlantCategory.SHOOTER;

    return switch (catStr.toLowerCase().trim()) {
      case "sun producers", "sun producer" -> PlantCategory.SUN_PRODUCER;
      case "shooters", "shooter" -> PlantCategory.SHOOTER;
      case "lobbers", "lobber" -> PlantCategory.LOBBER;
      case "explosives", "explosive" -> PlantCategory.EXPLOSIVE;
      case "wall-nuts", "wall-nut" -> PlantCategory.WALL_NUT;
      case "melee" -> PlantCategory.MELEE;
      case "modifier", "modifiers" -> PlantCategory.MODIFIER;
      case "strike-through" -> PlantCategory.STRIKE_THROUGH;
      case "homing" -> PlantCategory.HOMING;
      case "mint", "mints" -> PlantCategory.MINT;
      default -> PlantCategory.SHOOTER;
    };
  }

  private EnumSet<PlantTag> parseTags(String tagsStr) {
    EnumSet<PlantTag> tags = EnumSet.noneOf(PlantTag.class);
    if (tagsStr == null || tagsStr.isEmpty()) return tags;

    for (String rawTag : tagsStr.split(",")) {
      String token = rawTag.trim();
      if (token.isEmpty() || "-".equals(token)) {
        continue;
      }
      PlantTag tag = matchTag(token);
      if (tag != null) {
        tags.add(tag);
      } else {
        System.err.println("Warning: Unknown PlantTag '" + rawTag + "' ignored.");
      }
    }
    return tags;
  }

  private PlantTag matchTag(String token) {
    String normalized = token.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    if ("WRAMPUP".equals(normalized)) {
      normalized = "RAMPUP";
    }
    for (PlantTag candidate : PlantTag.values()) {
      if (candidate.name().replace("_", "").equals(normalized)) {
        return candidate;
      }
    }
    return null;
  }

  private double parseActionIntervalSeconds(String intervalStr) {
    if (intervalStr == null || intervalStr.trim().isEmpty() || "-".equals(intervalStr.trim())) {
      return DEFAULT_INTERVAL_SECONDS;
    }
    try {
      return Double.parseDouble(intervalStr.trim());
    } catch (NumberFormatException e) {
      System.err.println(
              "Warning: Invalid action interval '" + intervalStr + "', defaulting to "
                      + DEFAULT_INTERVAL_SECONDS + "s.");
      return DEFAULT_INTERVAL_SECONDS;
    }
  }

  private int parseDamage(String damageStr) {
    if (damageStr == null || damageStr.trim().isEmpty()) {
      return DEFAULT_DAMAGE;
    }
    String value = damageStr.trim();

    if (value.toLowerCase().contains("insta")) {
      return INSTA_KILL_DAMAGE;
    }
    if (value.contains("/")) {
      value = value.split("/")[0].trim();
    }
    if (value.toLowerCase().contains("x")) {
      String[] parts = value.toLowerCase().split("x");
      Integer perShot = tryParse(parts[0]);
      Integer shots = parts.length > 1 ? tryParse(parts[1]) : null;
      if (perShot != null && shots != null) {
        return perShot * shots;
      }
      if (perShot != null) {
        return perShot;
      }
      return DEFAULT_DAMAGE;
    }

    Integer parsed = tryParse(value);
    if (parsed == null) {
      System.err.println(
              "Warning: Invalid damage '" + damageStr + "', defaulting to " + DEFAULT_DAMAGE + ".");
      return DEFAULT_DAMAGE;
    }
    return parsed;
  }

  private Integer tryParse(String value) {
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static final Pattern SUN_AMOUNT =
          Pattern.compile("(\\d+)\\s*sun", Pattern.CASE_INSENSITIVE);

  /**
   * غذای گیاه برای گیاهان پشتیبان. Imitater طبق دیتا اثری ندارد (وابسته به گیاه کپی‌شده) و
   * null برمی‌گرداند تا پیام مناسب چاپ شود.
   */
  private PlantFood determineModifierPlantFood(PlantTemplate template) {
    String name = template.name == null ? "" : template.name.toLowerCase();
    if (name.contains("torchwood")) {
      return new PlantFood(1, new BlueFlameAction());
    }
    if (name.contains("hypno")) {
      return new PlantFood(150, new HypnoShroomAction(true));
    }
    if (name.contains("lily")) {
      return new PlantFood(1, new LilyPadSpreadAction(this, 3));
    }
    return null;
  }

  /** اولین عدد متن اثر غذای گیاه را برمی‌دارد (مثلا "4000 extra health" → 4000). */
  private int parseBonusHealth(String abilityText, int fallback) {
    if (abilityText == null) {
      return fallback;
    }
    Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(abilityText);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
  }

  private int parseSunAmount(String abilityText, int fallback) {
    if (abilityText == null) {
      return fallback;
    }
    Matcher matcher = SUN_AMOUNT.matcher(abilityText);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
  }

  private static final Pattern PARALLEL_LANES =
          Pattern.compile("(\\d+)\\s+parallel\\s+lanes", Pattern.CASE_INSENSITIVE);

  private int parseLaneSpread(String abilityText) {
    if (abilityText == null) {
      return 0;
    }
    Matcher matcher = PARALLEL_LANES.matcher(abilityText);
    if (!matcher.find()) {
      return 0;
    }
    return Math.max(0, (Integer.parseInt(matcher.group(1)) - 1) / 2);
  }

  private Projectile.ProjectileEffect resolveProjectileEffect(EnumSet<PlantTag> tags) {
    if (tags.contains(PlantTag.FIRE)) return Projectile.ProjectileEffect.FIRE;
    if (tags.contains(PlantTag.ICE)) return Projectile.ProjectileEffect.ICE;
    if (tags.contains(PlantTag.POISON)) return Projectile.ProjectileEffect.POISON;
    return Projectile.ProjectileEffect.NORMAL;
  }

  private PlantAction determineBehavior(
          PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags,
          PlantTemplate template) {
    // نعناع‌ها تو دیتا با دسته‌ی خانواده‌ی خودشون ثبت شدن، پس فقط از روی اسم قابل تشخیصن
    if (isMint(template.name)) {
      return new MintAction(5 * TICKS_PER_SECOND);
    }
    int power = category == PlantCategory.SUN_PRODUCER
            ? parseSunAmount(template.baseAbility, 25) : damage;
    if (tags.contains(PlantTag.RAMP_UP)) {
      return buildGrowthBehavior(category, interval, power, tags, template);
    }
    return baseBehaviorFor(category, interval, power, tags, template, 0);
  }

  // power برای تولیدکننده‌های خورشید یعنی مقدار خورشید و برای بقیه یعنی دمیج
  private PlantAction baseBehaviorFor(
          PlantCategory category, int interval, int power, EnumSet<PlantTag> tags,
          PlantTemplate template, int stageIndex) {
    return switch (category) {
      case SUN_PRODUCER -> new ProduceSunAction(interval, power);
      case SHOOTER -> buildShooter(interval, power, tags, false, template);
      case STRIKE_THROUGH -> buildShooter(interval, power, tags, true, template);
      case LOBBER -> new LobAction(interval, power, tags.contains(PlantTag.AOE), resolveProjectileEffect(tags));
      case MELEE -> new MeleeAction(interval, power, tags.contains(PlantTag.AOE) ? 1 + stageIndex : 0);
      case HOMING -> new HomingAction(interval, power);
      case EXPLOSIVE -> determineExplosiveBehavior(power, tags, template);
      case WALL_NUT -> determineWallNutBehavior(tags, template.name);
      case MODIFIER, MINT -> determineModifierBehavior(template);
      default -> throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    };
  }

  private ShootForwardAction buildShooter(int interval, int power, EnumSet<PlantTag> tags,
          boolean piercing, PlantTemplate template) {
    ShootForwardAction action = new ShootForwardAction(interval, power,
            resolveProjectileEffect(tags), piercing, parseLaneSpread(template.baseAbility));
    action.setDirections(parseFiringDirections(template.baseAbility));
    // گیاهانی مثل Bowling Bulb تیرهایشان بین ردیف‌ها کمانه می‌کند
    if (mentions(template.baseAbility, "ricochet")) {
      action.setRicochet(parseVanishSeconds(template.baseAbility) * TICKS_PER_SECOND);
    }
    return action;
  }

  private boolean isMint(String name) {
    return name != null && name.toLowerCase().endsWith("-mint");
  }

  private PlantAction determineModifierBehavior(PlantTemplate template) {
    String name = template.name == null ? "" : template.name.toLowerCase();
    if (name.contains("hypno")) {
      return new HypnoShroomAction();
    }
    // افکت Torchwood و Lily Pad رو Board و GameManager اعمال میکنن، پس رفتار تیکی لازم ندارن
    return null;
  }

  private PlantAction buildGrowthBehavior(
          PlantCategory category, int interval, int power, EnumSet<PlantTag> tags,
          PlantTemplate template) {
    int[] stageValues = parseStageValues(
            category == PlantCategory.SUN_PRODUCER ? template.baseAbility : template.damage);
    if (stageValues.length < 2) {
      return baseBehaviorFor(category, interval, power, tags, template, 0);
    }
    int[] stageStartTicks = parseStageStartTicks(template.baseAbility, stageValues.length);
    // بونوس لِوِل روی مقدار مرحله‌ی اول حساب شده؛ همون اختلاف رو به بقیه‌ی مرحله‌ها هم میدیم
    int levelBonus = power - stageValues[0];
    PlantAction[] stages = new PlantAction[stageValues.length];
    for (int i = 0; i < stageValues.length; i++) {
      stages[i] = baseBehaviorFor(
              category, interval, Math.max(0, stageValues[i] + levelBonus), tags, template, i);
    }
    return new GrowthStageAction(stages, stageStartTicks);
  }

  private static final Pattern STAGE_VALUES = Pattern.compile("(\\d+(?:/\\d+)+)");

  private int[] parseStageValues(String text) {
    if (text == null) {
      return new int[0];
    }
    Matcher matcher = STAGE_VALUES.matcher(text);
    if (!matcher.find()) {
      return new int[0];
    }
    String[] parts = matcher.group(1).split("/");
    int[] values = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Integer.parseInt(parts[i]);
    }
    return values;
  }

  private static final Pattern STAGE_TIME =
          Pattern.compile("Stg\\s*(\\d+)\\s*:\\s*(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);

  // "To Stg2: 24s | To Stg3: 72s" یعنی مرحله‌ها از تیک 0، 240 و 720 بعد از کاشت شروع میشن
  private int[] parseStageStartTicks(String abilityText, int stageCount) {
    int[] startTicks = new int[stageCount];
    if (abilityText == null) {
      return startTicks;
    }
    Matcher matcher = STAGE_TIME.matcher(abilityText);
    while (matcher.find()) {
      int stage = Integer.parseInt(matcher.group(1));
      if (stage >= 1 && stage <= stageCount) {
        startTicks[stage - 1] = Integer.parseInt(matcher.group(2)) * TICKS_PER_SECOND;
      }
    }
    return startTicks;
  }

  private static final int[][] DIAGONAL_DIRECTIONS = {{1, -1}, {1, 1}, {-1, -1}, {-1, 1}};
  private static final int[][] STAR_DIRECTIONS = {{1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}};
  private static final int[][] SPLIT_DIRECTIONS = {{1, 0}, {-1, 0}, {-1, 0}};

  // null یعنی همون شلیک مستقیم به جلو
  private int[][] parseFiringDirections(String abilityText) {
    if (abilityText == null) {
      return null;
    }
    String text = abilityText.toLowerCase();
    if (text.contains("diagonal")) {
      return DIAGONAL_DIRECTIONS;
    }
    if (text.contains("star-shaped")) {
      return STAR_DIRECTIONS;
    }
    if (text.contains("backward")) {
      return SPLIT_DIRECTIONS;
    }
    return null;
  }

  private PlantAction determineExplosiveBehavior(
          int damage, EnumSet<PlantTag> tags, PlantTemplate template) {
    int blastDamage = damage > 0 ? damage : 1800;
    int range = mentions(template.baseAbility, "lawn") ? 9 : (mentions(template.baseAbility, "3x3") ? 1 : 0);

    if (tags.contains(PlantTag.TRAP)) {
      int armSeconds = parseArmSeconds(template.baseAbility);
      return new ExplodeAction(armSeconds * TICKS_PER_SECOND, blastDamage, Math.max(range, 1), true);
    }

    ExplodeAction explode = new ExplodeAction(0, blastDamage, Math.max(range, 1));
    // گیاهانی مثل Grapeshot بعد از انفجار، ساچمه‌های کمانه‌کننده پخش می‌کنند
    if (mentions(template.baseAbility, "ricochet")) {
      explode.withScatteringGrapes(
              GRAPE_COUNT, parseVanishSeconds(template.baseAbility) * TICKS_PER_SECOND,
              Math.max(blastDamage / 6, 100));
    }
    return explode;
  }

  private static final int GRAPE_COUNT = 6;
  private static final int DEFAULT_GRAPE_LIFE_SECONDS = 5;

  /** از متن توانایی، «vanish after N seconds» را می‌خواند. */
  private int parseVanishSeconds(String abilityText) {
    if (abilityText == null) {
      return DEFAULT_GRAPE_LIFE_SECONDS;
    }
    Matcher matcher =
            java.util.regex.Pattern.compile("(\\d+)\\s*second").matcher(abilityText.toLowerCase());
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : DEFAULT_GRAPE_LIFE_SECONDS;
  }

  private static final Pattern ARM_SECONDS =
          Pattern.compile("(\\d+)\\s*second", Pattern.CASE_INSENSITIVE);

  private int parseArmSeconds(String abilityText) {
    if (abilityText == null) {
      return 15;
    }
    Matcher matcher = ARM_SECONDS.matcher(abilityText);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private boolean mentions(String text, String needle) {
    return text != null && text.toLowerCase().contains(needle.toLowerCase());
  }

  private PlantAction determineWallNutBehavior(EnumSet<PlantTag> tags, String name) {
    if (tags.contains(PlantTag.SUN)) return new SunOnHitAction(5);
    if (name != null && name.toLowerCase().contains("endurian")) return new ReflectDamageAction();
    if (tags.contains(PlantTag.MOVE_ZOMBIES)) {
      return new LaneRedirectAction(3);
    }
    return null;
  }

  private PlantFood determinePlantFood(
          PlantTemplate template, PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags) {
    if (isMint(template.name)) {
      return null;
    }
    return switch (category) {
      case SUN_PRODUCER ->
              new PlantFood(1, new ProduceSunAction(1, parseSunAmount(template.plantFoodEffect, 150)));
      case SHOOTER, STRIKE_THROUGH ->
              new PlantFood(150, buildShooter(Math.max(1, interval / 3), damage * 2, tags,
                      category == PlantCategory.STRIKE_THROUGH, template));
      case LOBBER ->
              new PlantFood(150,
                      new LobAction(Math.max(1, interval / 3), damage * 2, true, resolveProjectileEffect(tags)));
      case MELEE -> new PlantFood(150, new MeleeAction(Math.max(1, interval / 3), damage * 2));
      case HOMING -> new PlantFood(150, new HomingAction(Math.max(1, interval / 3), damage * 2));
      case WALL_NUT ->
              new PlantFood(1, new FortifyAction(parseBonusHealth(template.plantFoodEffect, 4000)));
      case EXPLOSIVE -> new PlantFood(1, new ExplodeAction(0, Math.max(damage * 2, 1800), 1));
      case MODIFIER -> determineModifierPlantFood(template);
      default -> new PlantFood(1,
              new DummyPlantAction("Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    };
  }
}