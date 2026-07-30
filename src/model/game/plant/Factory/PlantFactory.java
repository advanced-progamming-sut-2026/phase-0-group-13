package model.game.plant.Factory;

import data.repository.PlantRepository;
import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.PlantFood;
import model.game.Projectile;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.*;

public class PlantFactory {
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

    int baseInterval = parseActionInterval(template.actionInterval);
    int baseDamage = parseDamage(template.damage);

    PlantLevel levelStats = PlantLevel.cumulative(template, level);

    int interval = Math.max(1, baseInterval + levelStats.getActionIntervalDeltaSeconds());
    int damage = Math.max(0, baseDamage + levelStats.getDamageDelta());
    int hp = Math.max(1, template.baseHp + levelStats.getHpDelta());
    int cost = Math.max(0, template.cost + levelStats.getCostDelta());

    PlantAction behavior = determineBehavior(category, interval, damage, tags, template.name);
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

    for (String tag : tagsStr.split(",")) {
      try {
        tags.add(PlantTag.valueOf(tag.trim().toUpperCase()));
      } catch (IllegalArgumentException e) {
        System.err.println("Warning: Unknown PlantTag '" + tag + "' ignored.");
      }
    }
    return tags;
  }

  private int parseActionInterval(String intervalStr) {
    try {
      if (intervalStr != null && !intervalStr.trim().isEmpty()) {
        return Integer.parseInt(intervalStr.trim());
      }
    } catch (NumberFormatException e) {
      System.err.println("Warning: Invalid action interval parsed, defaulting to 30.");
    }
    return 30;
  }

  private int parseDamage(String damageStr) {
    try {
      if (damageStr != null && !damageStr.trim().isEmpty()) {
        return Integer.parseInt(damageStr.trim());
      }
    } catch (NumberFormatException e) {
      System.err.println("Warning: Invalid damage parsed, defaulting to 20.");
    }
    return 20;
  }

  private Projectile.ProjectileEffect resolveProjectileEffect(EnumSet<PlantTag> tags) {
    if (tags.contains(PlantTag.FIRE)) return Projectile.ProjectileEffect.FIRE;
    if (tags.contains(PlantTag.ICE)) return Projectile.ProjectileEffect.ICE;
    if (tags.contains(PlantTag.POISON)) return Projectile.ProjectileEffect.POISON;
    return Projectile.ProjectileEffect.NORMAL;
  }

  private PlantAction determineBehavior(
          PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags, String name) {
    return switch (category) {
      case SUN_PRODUCER -> new ProduceSunAction(interval);
      case SHOOTER -> new ShootForwardAction(interval, damage, resolveProjectileEffect(tags));
      case STRIKE_THROUGH -> new ShootForwardAction(interval, damage, resolveProjectileEffect(tags), true);
      case LOBBER -> new LobAction(interval, damage, tags.contains(PlantTag.AOE), resolveProjectileEffect(tags));
      case MELEE -> new MeleeAction(interval, damage);
      case HOMING -> new HomingAction(interval, damage);
      case EXPLOSIVE -> new ExplodeAction();
      case WALL_NUT -> determineWallNutBehavior(tags, name);
      case MODIFIER, MINT -> new DummyPlantAction("Category " + category + " has no real behavior class yet");
      default -> throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    };
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
      case SUN_PRODUCER -> new PlantFood(1, new ProduceSunAction(1));
      case SHOOTER -> new PlantFood(150, new ShootForwardAction(Math.max(1, interval / 3), damage * 2, resolveProjectileEffect(tags)));
      default -> new PlantFood(1, new DummyPlantAction("Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    };
  }
}