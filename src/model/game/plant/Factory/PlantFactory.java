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

  private int parseSunAmount(String abilityText, int fallback) {
    if (abilityText == null) {
      return fallback;
    }
    Matcher matcher = SUN_AMOUNT.matcher(abilityText);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
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
    return switch (category) {
      case SUN_PRODUCER -> new ProduceSunAction(interval, parseSunAmount(template.baseAbility, 25));
      case SHOOTER -> new ShootForwardAction(interval, damage, resolveProjectileEffect(tags));
      case STRIKE_THROUGH -> new ShootForwardAction(interval, damage, resolveProjectileEffect(tags), true);
      case LOBBER -> new LobAction(interval, damage, tags.contains(PlantTag.AOE), resolveProjectileEffect(tags));
      case MELEE -> new MeleeAction(interval, damage);
      case HOMING -> new HomingAction(interval, damage);
      case EXPLOSIVE -> determineExplosiveBehavior(damage, tags, template);
      case WALL_NUT -> determineWallNutBehavior(tags, template.name);
      case MODIFIER, MINT -> new DummyPlantAction("Category " + category + " has no real behavior class yet");
      default -> throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    };
  }

  private PlantAction determineExplosiveBehavior(
          int damage, EnumSet<PlantTag> tags, PlantTemplate template) {
    int blastDamage = damage > 0 ? damage : 1800;
    int range = mentions(template.baseAbility, "3x3") ? 1 : 0;

    if (tags.contains(PlantTag.TRAP)) {
      int armSeconds = parseArmSeconds(template.baseAbility);
      return new ExplodeAction(armSeconds * TICKS_PER_SECOND, blastDamage, Math.max(range, 1));
    }
    return new ExplodeAction(0, blastDamage, Math.max(range, 1));
  }

  private static final Pattern ARM_SECONDS =
          Pattern.compile("(\\d+)\\s*second", Pattern.CASE_INSENSITIVE);

  private int parseArmSeconds(String abilityText) {
    if (abilityText == null) {
      return 15;
    }
    Matcher matcher = ARM_SECONDS.matcher(abilityText);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 15;
  }

  private boolean mentions(String text, String needle) {
    return text != null && text.toLowerCase().contains(needle.toLowerCase());
  }

  private PlantAction determineWallNutBehavior(EnumSet<PlantTag> tags, String name) {
    if (tags.contains(PlantTag.SUN)) return new SunOnHitAction(5);
    if (name != null && name.toLowerCase().contains("endurian")) return new ReflectDamageAction();
    if (tags.contains(PlantTag.MOVE_ZOMBIES)) {
      return new DummyPlantAction("Lane-redirect needs Zombie row to become mutable");
    }
    return null;
  }

  private PlantFood determinePlantFood(
          PlantTemplate template, PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags) {
    return switch (category) {
      case SUN_PRODUCER ->
              new PlantFood(1, new ProduceSunAction(1, parseSunAmount(template.plantFoodEffect, 150)));
      case SHOOTER, STRIKE_THROUGH ->
              new PlantFood(150,
                      new ShootForwardAction(Math.max(1, interval / 3), damage * 2, resolveProjectileEffect(tags)));
      case LOBBER ->
              new PlantFood(150,
                      new LobAction(Math.max(1, interval / 3), damage * 2, true, resolveProjectileEffect(tags)));
      case MELEE -> new PlantFood(150, new MeleeAction(Math.max(1, interval / 3), damage * 2));
      case HOMING -> new PlantFood(150, new HomingAction(Math.max(1, interval / 3), damage * 2));
      default -> new PlantFood(1,
              new DummyPlantAction("Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    };
  }
}