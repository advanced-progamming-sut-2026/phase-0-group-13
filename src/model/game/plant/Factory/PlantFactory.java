package model.game.plant.Factory;

import data.repository.PlantRepository;
import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.PlantFood;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.plant.behavior.*;

public class PlantFactory {
  private final PlantRepository repository;

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
    PlantFood plantFood = determinePlantFood(template, category, interval, damage);

    return new Plant(template, row, col, category, tags, behavior, plantFood);
  }

  private PlantCategory determineCategory(String catStr) {
    if (catStr == null) return PlantCategory.SHOOTER;
    switch (catStr.toLowerCase().trim()) {
      case "sun producers":
      case "sun producer":
        return PlantCategory.SUN_PRODUCER;
      case "shooters":
      case "shooter":
        return PlantCategory.SHOOTER;
      case "lobbers":
      case "lobber":
        return PlantCategory.LOBBER;
      case "explosives":
      case "explosive":
        return PlantCategory.EXPLOSIVE;
      case "wall-nuts":
      case "wall-nut":
        return PlantCategory.WALL_NUT;
      case "melee":
        return PlantCategory.MELEE;
      case "modifier":
      case "modifiers":
        return PlantCategory.MODIFIER;
      case "strike-through":
        return PlantCategory.STRIKE_THROUGH;
      case "homing":
        return PlantCategory.HOMING;
      case "mint":
      case "mints":
        return PlantCategory.MINT;
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



  // بقیش باید کامل بشه
  //
  private PlantAction determineBehavior(PlantCategory category, int interval, int damage) {
    switch (category) {
      case SUN_PRODUCER:
        return new ProduceSunAction(interval);
      case SHOOTER:
        return new ShootForwardAction(interval, damage);
      case EXPLOSIVE:
        return new ExplodeAction();
      case WALL_NUT:
        return null;
      case LOBBER:
      case MELEE:
      case MODIFIER:
      case STRIKE_THROUGH:
      case HOMING:
      case MINT:
        return new DummyPlantAction("category " + category + " has no real behavior class yet");
      default:
        throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    }
  }


  private PlantFood determinePlantFood(
          PlantTemplate template, PlantCategory category, int interval, int damage) {
    switch (category) {
      case SUN_PRODUCER:
        return new PlantFood(1, new ProduceSunAction(1));
      case SHOOTER:
        return new PlantFood(150, new ShootForwardAction(Math.max(1, interval / 3), damage * 2));
      default:
        return new PlantFood(
                1,
                new DummyPlantAction(
                        "Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    }
  }
}