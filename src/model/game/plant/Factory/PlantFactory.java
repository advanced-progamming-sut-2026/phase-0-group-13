package model.game.plant.Factory;

import data.repository.PlantRepository; // اضافه شدن ریپازیتوری
import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.PlantFood;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.*;

public class PlantFactory {
  private PlantRepository repository;

  public PlantFactory(PlantRepository repository) {
    this.repository = repository;
  }

  public Plant createPlant(String name, int row, int col) {
    PlantTemplate template = this.repository.find(name.toLowerCase());

    if (template == null) {
      System.err.println("قالب گیاه یافت نشد: " + name);
      return null;
    }

    PlantCategory category = determineCategory(template.category);
    EnumSet<PlantTag> tags = parseTags(template.tags);

    int interval = parseActionInterval(template.actionInterval);
    int damage = parseDamage(template.damage);

    PlantAction behavior = determineBehavior(category, interval, damage);
    PlantFood plantFood = determinePlantFood(category);

    return new Plant(template, row, col, category, tags, behavior, plantFood);
  }

  private PlantCategory determineCategory(String catStr) {
    if (catStr == null) return PlantCategory.SHOOTER;
    switch (catStr.toLowerCase().trim()) {
      case "sun producers":
        return PlantCategory.SUN_PRODUCER;
      case "shooters":
        return PlantCategory.SHOOTER;
      case "lobbers":
        return PlantCategory.LOBBER;
      case "explosives":
        return PlantCategory.EXPLOSIVE;
      case "wall-nuts":
        return PlantCategory.WALL_NUT;
      case "melee":
        return PlantCategory.MELEE;
      default:
        return PlantCategory.SHOOTER;
    }
  }

  private EnumSet<PlantTag> parseTags(String tagsStr) {
    EnumSet<PlantTag> tags = EnumSet.noneOf(PlantTag.class);
    if (tagsStr == null || tagsStr.isEmpty()) return tags;

    for (String tag : tagsStr.split(",")) {
      try {
        tags.add(PlantTag.valueOf(tag.trim().toUpperCase()));
      } catch (IllegalArgumentException e) {
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
    }
    return 30;
  }

  private int parseDamage(String damageStr) {
    try {
      if (damageStr != null && !damageStr.trim().isEmpty()) {
        return Integer.parseInt(damageStr.trim());
      }
    } catch (NumberFormatException e) {
    }
    return 20;
  }

  private PlantAction determineBehavior(PlantCategory category, int interval, int damage) {
    switch (category) {
      case SUN_PRODUCER:
        return new ProduceSunAction(interval);
      case SHOOTER:
        return new ShootForwardAction(interval, damage);
      case EXPLOSIVE:
        return new ExplodeAction();
      default:
        return null;
    }
  }

  private PlantFood determinePlantFood(PlantCategory category) {
    // TODO
    return null;
  }
}
