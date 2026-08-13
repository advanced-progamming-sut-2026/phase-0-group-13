package model.game.plant.Factory;

import data.repository.PlantRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
    String imitated = parseImitationTarget(name);
    if (imitated != null) {
      return createImitation(imitated, row, col, level);
    }

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
    // "Atk Speed +N%" یعنی بازه‌ی عمل به همان نسبت کوتاه‌تر می‌شود
    intervalSeconds =
            Math.max(0.1, intervalSeconds * (1.0 - levelStats.getAttackSpeedPercent() / 100.0));
    int interval = Math.max(1, (int) Math.round(intervalSeconds * TICKS_PER_SECOND));

    int damage = Math.max(0, baseDamage + levelStats.getDamageDelta());
    int hp = Math.max(1, template.baseHp + levelStats.getHpDelta());
    int cost = Math.max(0, template.cost + levelStats.getCostDelta());

    this.levelStats = levelStats;
    PlantAction behavior = determineBehavior(category, interval, damage, tags, template);
    PlantFood plantFood = determinePlantFood(template, category, interval, damage, tags);

    Plant plant = new Plant(template, row, col, category, tags, behavior, plantFood, level, hp, cost);
    applyLifespan(plant, template, levelStats);
    return plant;
  }

  /**
   * دسته‌های لِوِلی که تازه اعمال می‌شوند (سرعت حمله، خورشید، پیرس، مدت یخ و ...) در چند نقطه از
   * ساخت رفتار لازم‌اند؛ به جای پاس‌دادن به همه‌ی متدها، همان لِوِلِ در دست ساخت اینجا نگه می‌ماند.
   */
  private PlantLevel levelStats = PlantLevel.none();

  private static final Pattern LIFESPAN_SECONDS =
          Pattern.compile("lifespan of (\\d+) second", Pattern.CASE_INSENSITIVE);

  /** «Limited lifespan of 60 seconds» در دیتای Sea-shroom و Puff-shroom. */
  private void applyLifespan(Plant plant, PlantTemplate template, PlantLevel stats) {
    if (template.baseAbility == null) {
      return;
    }
    Matcher matcher = LIFESPAN_SECONDS.matcher(template.baseAbility);
    if (!matcher.find()) {
      return;
    }
    int seconds = Integer.parseInt(matcher.group(1)) + stats.getLifespanDeltaSeconds();
    plant.setLifespanTicks(Math.max(1, seconds) * TICKS_PER_SECOND);
  }

  private static final Pattern IMITATER_TARGET =
          Pattern.compile("^imitater\\s*[:\\-]?\\s*(.+)$", Pattern.CASE_INSENSITIVE);

  private static final int IMITATER_FREE_PLANT_FOOD_LEVEL = 4;

  /**
   * Imitater در plants.json «گیاه دیگری را کپی می‌کند (تا بتوانی یک کارت را دو بار بازی کنی)».
   * چون دستور کاشت (`plant plant -t <type> ...`) متن آزاد می‌گیرد، کافی است نامِ هدف را پشت
   * «imitater» بنویسند؛ نیازی به دستور یا رابط جدید نیست.
   *
   * @return نام گیاه کپی‌شونده، یا null اگر این اسم اصلا Imitater نباشد
   */
  private String parseImitationTarget(String name) {
    if (name == null) {
      return null;
    }
    Matcher matcher = IMITATER_TARGET.matcher(name.trim());
    if (!matcher.matches()) {
      return null;
    }
    String target = matcher.group(1).trim();
    return target.isEmpty() ? null : target;
  }

  /**
   * کپی دقیقا همان گیاه هدف است (همان رفتار، همان غذای گیاه، همان جان). Lvl 4 دیتای Imitater
   * «plant food on enterance» است، پس از آن لِوِل به بعد کپی با غذای گیاهِ فعال متولد می‌شود.
   */
  private Plant createImitation(String targetName, int row, int col, int level) {
    Plant copy = createPlant(targetName, row, col, level);
    if (copy == null) {
      System.err.println("Imitater cannot copy unknown plant: " + targetName);
      return null;
    }
    if (level >= IMITATER_FREE_PLANT_FOOD_LEVEL && copy.hasPlantFoodEffect()) {
      copy.applyPlantFood();
    }
    System.out.printf("Imitater copied %s.%n", copy.getName());
    return copy;
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
      // "Duration +1s" در لِوِل‌های نعناع تا حالا اعمال نمی‌شد
      return new MintAction(
              (MINT_BASE_DURATION_SECONDS + levelStats.getDurationDeltaSeconds())
                      * TICKS_PER_SECOND);
    }
    // Bowling Bulb سه نوع پیاز با دمیج و تاخیر جدا دارد ("40/120/180"، "Cyan/Blue/Orange")
    int[] bulbDamages = parseStageValues(template.damage);
    if (bulbDamages.length == 3 && mentions(template.baseAbility, "3 kinds of bulb")) {
      return buildBowlingBulb(template, bulbDamages);
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
      // Gold Bloom در دیتا «یک‌بار خورشید می‌دهد و ناپدید می‌شود»؛ بقیه چرخه‌ای تولید می‌کنند
      case SUN_PRODUCER -> new ProduceSunAction(
              interval, power + levelStats.getSunDelta(),
              mentions(template.baseAbility, "vanishes"));
      case SHOOTER -> buildShooter(interval, power, tags, false, template);
      case STRIKE_THROUGH -> buildShooter(interval, power, tags, true, template);
      case LOBBER -> buildLobber(interval, power, tags, template);
      case MELEE -> new MeleeAction(interval, power,
              tags.contains(PlantTag.AOE) ? 1 + stageIndex + levelStats.getRangeDelta() : 0,
              mentions(template.baseAbility, "behind"));
      case HOMING -> determineHomingBehavior(interval, power, template);
      case EXPLOSIVE -> determineExplosiveBehavior(power, tags, template);
      case WALL_NUT -> determineWallNutBehavior(tags, template.name);
      case MODIFIER, MINT -> determineModifierBehavior(template);
      default -> throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    };
  }

  private static final Pattern BUTTER_BONUS =
          Pattern.compile("butter\\s*\\+(\\d+)%", Pattern.CASE_INSENSITIVE);

  /**
   * Kernel-pult تنها لابری است که دیتا برایش دو نوع پرتابه ثبت کرده ("20/40": دانه یا کره‌ی
   * گیج‌کننده). بقیه‌ی لابرها همان LobAction قبلی را می‌گیرند.
   */
  private PlantAction buildLobber(int interval, int power, EnumSet<PlantTag> tags,
          PlantTemplate template) {
    int[] shotValues = parseStageValues(template.damage);
    if (mentions(template.baseAbility, "butter") && shotValues.length == 2) {
      int bonus = butterChanceBonus(template);
      return new ButterLobAction(interval, shotValues[0] + levelStats.getDamageDelta(),
              shotValues[1] + levelStats.getDamageDelta(),
              ButterLobAction.BASE_BUTTER_CHANCE_PERCENT + bonus, false);
    }
    return new LobAction(interval, power, tags.contains(PlantTag.AOE),
            resolveProjectileEffect(tags));
  }

  /** «Butter +5%» فقط از لِوِل ۲ به بعد فعال است. */
  private int butterChanceBonus(PlantTemplate template) {
    int bonus = 0;
    for (String effect : levelStats.getRawEffects()) {
      Matcher matcher = BUTTER_BONUS.matcher(effect);
      if (matcher.find()) {
        bonus += Integer.parseInt(matcher.group(1));
      }
    }
    return bonus;
  }

  private static final int MINT_BASE_DURATION_SECONDS = 5;

  private static final Pattern BULB_DELAY =
          Pattern.compile("([A-Za-z]+)\\s*:\\s*(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);

  /**
   * «Fires 3 kinds of bulb that ricochet between lanes ... Delay per bulb Cyan: 2s | Blue: 5s |
   * Orange: 10s» با دمیج "40/120/180" — هر پیاز تایمر خودش را دارد.
   */
  private PlantAction buildBowlingBulb(PlantTemplate template, int[] damages) {
    List<String> names = new ArrayList<>();
    List<Integer> delays = new ArrayList<>();
    Matcher matcher = BULB_DELAY.matcher(template.baseAbility);
    while (matcher.find() && names.size() < damages.length) {
      names.add(matcher.group(1));
      delays.add(Integer.parseInt(matcher.group(2)) * TICKS_PER_SECOND);
    }
    if (names.size() < damages.length) {
      return null;
    }

    int[] intervals = new int[damages.length];
    int[] levelledDamage = new int[damages.length];
    for (int i = 0; i < damages.length; i++) {
      intervals[i] = delays.get(i);
      levelledDamage[i] = Math.max(0, damages[i] + levelStats.getDamageDelta());
    }
    return new MultiBulbAction(names.toArray(new String[0]), levelledDamage, intervals,
            parseVanishSeconds(template.baseAbility) * TICKS_PER_SECOND);
  }

  private static final double MAGNET_RANGE = 3.0;

  /**
   * دستهٔ Homing فقط شلیکِ ردیاب نیست: طبق plants.json کارِ Caulipower هیپنوتیزم‌کردنِ هدف است و
   * کارِ Magnet-shroom کندنِ فلز از سر زامبی — هیچ‌کدام تیر با دمیج شلیک نمی‌کنند.
   */
  private PlantAction determineHomingBehavior(int interval, int power, PlantTemplate template) {
    String name = template.name == null ? "" : template.name.toLowerCase();
    if (mentions(template.baseAbility, "hypnotis")) {
      return new HypnotiseAction(interval, 1);
    }
    if (name.contains("magnet")) {
      return new MagnetAction(interval, 1, MAGNET_RANGE);
    }
    return new HomingAction(interval, power);
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
    action.setPierceLimit(parsePierceLimit(template.baseAbility));
    return action;
  }

  private static final Pattern PIERCE_COUNT =
          Pattern.compile("pierces through (\\d+)", Pattern.CASE_INSENSITIVE);

  /**
   * Cactus در دیتا «از ۳ زامبی رد می‌شود» و Lvl 2 یکی اضافه می‌کند. Fume-shroom عددی ندارد، پس
   * صفر (بی‌نهایت) برمی‌گردد و رفتار فعلی‌اش دست‌نخورده می‌ماند.
   */
  private int parsePierceLimit(String abilityText) {
    if (abilityText == null) {
      return 0;
    }
    Matcher matcher = PIERCE_COUNT.matcher(abilityText);
    if (!matcher.find()) {
      return 0;
    }
    return Math.max(1, Integer.parseInt(matcher.group(1)) + levelStats.getPierceDelta());
  }

  private boolean isMint(String name) {
    return name != null && name.toLowerCase().endsWith("-mint");
  }

  private boolean hasNoPlantFoodEffect(String plantFoodEffect) {
    return plantFoodEffect != null && plantFoodEffect.trim().toLowerCase().startsWith("none");
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
    // گیاهانی که دیتا برایشان دمیج صفر و کارِ «یخ‌زدن» ثبت کرده (Iceberg Lettuce, Ice-shroom)
    // نباید فالبکِ ۱۸۰۰ دمیج بگیرند؛ کارشان freeze است نه انفجار
    if (mentions(template.baseAbility, "freeze")) {
      boolean wholeLawn = mentions(template.baseAbility, "lawn")
              || mentions(template.baseAbility, "every zombie");
      return freezeAction(wholeLawn, true);
    }
    // Hot Potato یخ را آب می‌کند و Grave Buster سنگ‌قبر را نابود می‌کند؛ هیچ‌کدام منفجر نمی‌شوند
    if (mentions(template.baseAbility, "melt it")) {
      return new MeltIceAction(levelStats.getRangeDelta());
    }
    if (mentions(template.baseAbility, "gravestone")) {
      return new GraveBusterAction(
              Math.max(0, GRAVE_BUSTER_EAT_SECONDS + levelStats.getActionIntervalDeltaSeconds())
                      * TICKS_PER_SECOND);
    }

    int blastDamage = damage > 0 ? damage : 1800;
    int range = mentions(template.baseAbility, "lawn") ? 9 : (mentions(template.baseAbility, "3x3") ? 1 : 0);

    // Jalapeno: «هر زامبی در یک ردیف» یعنی کل ردیف، نه ناحیهٔ ۳x۳
    if (mentions(template.baseAbility, "in one lane")) {
      return new ExplodeAction(0, blastDamage, range).asLaneWide();
    }
    // Doom-shroom گودال غیرقابل کاشت به جا می‌گذارد
    if (mentions(template.baseAbility, "crater")) {
      return new ExplodeAction(0, blastDamage, Math.max(range, 1)).leavingCrater();
    }

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

  private static final int GRAVE_BUSTER_EAT_SECONDS = 3;

  /** مدت یخ‌زدگی: پایه‌ی FreezeAction به‌علاوهٔ «Freeze Time +2s» لِوِل. */
  private FreezeAction freezeAction(boolean wholeLawn, boolean consumedOnUse) {
    int seconds = FreezeAction.DEFAULT_FREEZE_SECONDS + levelStats.getFreezeTimeDeltaSeconds();
    return new FreezeAction(Math.max(1, seconds) * TICKS_PER_SECOND, wholeLawn, consumedOnUse);
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
      // Sweet Potato آهنرباست و زامبی‌ها را به ردیف خودش می‌کِشد؛ Garlic برعکس، هُلشان می‌دهد
      boolean magnet = name != null && name.toLowerCase().contains("sweet potato");
      return magnet ? new LaneMagnetAction(20) : new LaneRedirectAction(3);
    }
    return null;
  }

  private PlantFood determinePlantFood(
          PlantTemplate template, PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags) {
    // plants.json برای گیاهان یک‌بارمصرف (Cherry Bomb, Jalapeno, Gold Bloom, ...) صراحتا
    // "None (single-use plant)" ثبت کرده؛ پس نباید برایشان اثر ساختگی بسازیم
    if (isMint(template.name) || hasNoPlantFoodEffect(template.plantFoodEffect)) {
      return null;
    }
    return switch (category) {
      case SUN_PRODUCER ->
              new PlantFood(1, new ProduceSunAction(1, parseSunAmount(template.plantFoodEffect, 150)));
      case SHOOTER, STRIKE_THROUGH -> buildShooterPlantFood(template, category, interval, damage, tags);
      case LOBBER -> buildLobberPlantFood(template, interval, damage, tags);
      case MELEE -> new PlantFood(150, new MeleeAction(
              Math.max(1, interval / 3), damage * 2, plantFoodMeleeRadius(template)));
      case HOMING -> determineHomingPlantFood(template, interval, damage);
      case WALL_NUT -> determineWallNutPlantFood(template, tags);
      case EXPLOSIVE -> determineExplosivePlantFood(template, damage);
      case MODIFIER -> determineModifierPlantFood(template);
      default -> new PlantFood(1,
              new DummyPlantAction("Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    };
  }

  private static final int PLANT_FOOD_TARGETS = 3;

  /**
   * دو اثر غذای گیاهِ شوترها که فقط رگبار نبودند: Fume-shroom «زامبی‌ها را عقب می‌راند» و
   * قارچ‌های عمر-محدود «عمر همهٔ هم‌نوع‌هایشان را از نو می‌کنند».
   */
  private PlantFood buildShooterPlantFood(PlantTemplate template, PlantCategory category,
          int interval, int damage, EnumSet<PlantTag> tags) {
    boolean piercing = category == PlantCategory.STRIKE_THROUGH;
    if (mentions(template.plantFoodEffect, "pushes zombies back")) {
      return new PlantFood(1, new FumeBlastAction(damage * 2, piercing));
    }
    ShootForwardAction burst =
            buildShooter(Math.max(1, interval / 3), damage * 2, tags, piercing, template);
    if (mentions(template.plantFoodEffect, "lifespan")) {
      return new PlantFood(150, new ResetLifespanAction(burst));
    }
    return new PlantFood(150, burst);
  }

  /** اثر غذای گیاهِ Kernel-pult: «روی سر هر زامبیِ روی زمین کره می‌اندازد». */
  private PlantFood buildLobberPlantFood(PlantTemplate template, int interval, int damage,
          EnumSet<PlantTag> tags) {
    int[] shotValues = parseStageValues(template.damage);
    if (mentions(template.plantFoodEffect, "butter") && shotValues.length == 2) {
      return new PlantFood(1, new ButterLobAction(1, shotValues[0], shotValues[1], 100, true));
    }
    return new PlantFood(150,
            new LobAction(Math.max(1, interval / 3), damage * 2, true, resolveProjectileEffect(tags)));
  }

  /**
   * اثر غذای گیاهِ Bonk Choy / Wasabi Whip / Phat Beet / Kiwibeast در دیتا صراحتا ناحیه‌ای است
   * ("3x3 area" / "area damage" / "every nearby zombie") ولی سازندهٔ دوآرگومانی شعاع را صفر
   * می‌گذاشت. Chomper استثناست: دیتایش «۳ زامبی» است، نه ناحیه — پس تک‌هدف می‌ماند.
   */
  private int plantFoodMeleeRadius(PlantTemplate template) {
    String effect = template.plantFoodEffect;
    boolean areaEffect = mentions(effect, "3x3")
            || mentions(effect, "area")
            || mentions(effect, "nearby");
    return areaEffect ? 1 : 0;
  }

  /** Caulipower چند زامبی تصادفی را هیپنوتیزم می‌کند و Magnet-shroom چند فلز را یکجا می‌کَند. */
  private PlantFood determineHomingPlantFood(PlantTemplate template, int interval, int damage) {
    String name = template.name == null ? "" : template.name.toLowerCase();
    if (mentions(template.plantFoodEffect, "hypnotis")) {
      return new PlantFood(1, new HypnotiseAction(1, PLANT_FOOD_TARGETS));
    }
    if (name.contains("magnet")) {
      return new PlantFood(1, new MagnetAction(1, PLANT_FOOD_TARGETS, MAGNET_RANGE));
    }
    return new PlantFood(150, new HomingAction(Math.max(1, interval / 3), damage * 2));
  }

  /**
   * گردوهای تگ moveZombies اثرشان زره نیست: Garlic کل ردیف را بیرون می‌ریزد و Sweet Potato
   * زامبی‌های نزدیک را می‌کِشد و جانش را کامل می‌کند.
   */
  private PlantFood determineWallNutPlantFood(PlantTemplate template, EnumSet<PlantTag> tags) {
    if (tags.contains(PlantTag.MOVE_ZOMBIES)) {
      boolean magnet = template.name != null
              && template.name.toLowerCase().contains("sweet potato");
      return magnet
              ? new PlantFood(1, new LaneMagnetAction(1, true))
              : new PlantFood(1, new LaneRedirectAction(Integer.MAX_VALUE, true));
    }
    return new PlantFood(1, new FortifyAction(parseBonusHealth(template.plantFoodEffect, 4000)));
  }

  /** Iceberg Lettuce با غذای گیاه کل زمین را یخ می‌زند؛ بقیه‌ی انفجاری‌ها منفجر می‌شوند. */
  private PlantFood determineExplosivePlantFood(PlantTemplate template, int damage) {
    if (mentions(template.plantFoodEffect, "freeze")) {
      return new PlantFood(1, freezeAction(true, true));
    }
    return new PlantFood(1, new ExplodeAction(0, Math.max(damage * 2, 1800), 1));
  }
}