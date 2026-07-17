package model.game.plant.Factory;

import data.repository.PlantRepository;
import java.util.EnumSet;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.game.PlantFood;
import model.game.Projectile;
import model.game.plant.Plant;
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
      System.err.println("قالب گیاه یافت نشد: " + name);
      return null;
    }

    PlantCategory category = determineCategory(template.category);
    EnumSet<PlantTag> tags = parseTags(template.tags);

    int baseInterval = parseActionInterval(template.actionInterval);
    int baseDamage = parseDamage(template.damage);

    model.game.plant.PlantParts.PlantLevel levelStats =
            model.game.plant.PlantParts.PlantLevel.cumulative(template, level);

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



  // افکت تیر (آتشین/یخی/سمی) بر اساس تگ گیاه انتخاب میشه؛ هم شوتر معمولی هم استرایک-ثرو ازش استفاده میکنن
  private Projectile.ProjectileEffect resolveProjectileEffect(EnumSet<PlantTag> tags) {
    if (tags.contains(PlantTag.FIRE)) return Projectile.ProjectileEffect.FIRE;
    if (tags.contains(PlantTag.ICE)) return Projectile.ProjectileEffect.ICE;
    if (tags.contains(PlantTag.POISON)) return Projectile.ProjectileEffect.POISON;
    return Projectile.ProjectileEffect.NORMAL;
  }

  // بقیش باید کامل بشه
  //
  // MODIFIER پیاده نشده چون هر گیاهش (Torchwood/Hypno-shroom/Imitater/Lily Pad) کارکرد کاملا متفاوتی
  // داره و یه استراتژی مشترک براشون معنی نداره؛ هرکدوم باید کلاس اختصاصی خودش رو بگیره.
  // MINT هم چون تو plants.json هیچ گیاهی این دسته رو نداره (گیاهای mint-family زیر دسته خودشون افتادن)
  // فعلا استاب مونده.
  private PlantAction determineBehavior(
          PlantCategory category, int interval, int damage, EnumSet<PlantTag> tags, String name) {
    switch (category) {
      case SUN_PRODUCER:
        return new ProduceSunAction(interval);
      case SHOOTER:
        return new ShootForwardAction(interval, damage, resolveProjectileEffect(tags));
      case STRIKE_THROUGH:
        return new ShootForwardAction(interval, damage, resolveProjectileEffect(tags), true);
      case LOBBER:
        return new LobAction(interval, damage, tags.contains(PlantTag.AOE), resolveProjectileEffect(tags));
      case MELEE:
        return new MeleeAction(interval, damage);
      case HOMING:
        return new HomingAction(interval, damage);
      case EXPLOSIVE:
        return new ExplodeAction();
      case WALL_NUT:
        return determineWallNutBehavior(tags, name);
      case MODIFIER:
      case MINT:
        return new DummyPlantAction("category " + category + " has no real behavior class yet");
      default:
        throw new UnsupportedOperationException("Unknown PlantCategory: " + category);
    }
  }

  // اکثر Wall-nut ها واقعا صرفا سپر منفعلن (null درسته براشون)، ولی چندتاشون قابلیت فعال دارن که با
  // تگ/اسم تشخیص داده میشن؛ اونایی که فعلا زیرساخت لازم رو ندارن (رج عوض کردن زامبی، انفجار موقع مرگ)
  // با DummyPlantAction به‌جای null صادقانه علامت‌گذاری میشن که با سپر واقعی اشتباه گرفته نشن
  private PlantAction determineWallNutBehavior(EnumSet<PlantTag> tags, String name) {
    if (tags.contains(PlantTag.SUN)) {
      return new SunOnHitAction(5);
    }
    if (name != null && name.toLowerCase().contains("endurian")) {
      return new ReflectDamageAction();
    }
    if (tags.contains(PlantTag.MOVE_ZOMBIES)) {
      return new DummyPlantAction(
              "lane-redirect needs Zombie's row to become mutable (Person A's Board contract)");
    }
    if (tags.contains(PlantTag.EXPLOSIVE)) {
      // این گیاه وقتی زنده‌ست کاری نمی‌کنه؛ لحظه‌ی مرگش Board.triggerDeathExplosions() تشخیص میده
      // و خودش ExplodeAction رو روی مختصات گیاه اجرا میکنه (نیازی به رفتار فعال نیست).
      return null;
    }
    return null;
  }


  private PlantFood determinePlantFood(
          PlantTemplate template,
          PlantCategory category,
          int interval,
          int damage,
          EnumSet<PlantTag> tags) {
    switch (category) {
      case SUN_PRODUCER:
        return new PlantFood(1, new ProduceSunAction(1));
      case SHOOTER:
        return new PlantFood(
                150,
                new ShootForwardAction(
                        Math.max(1, interval / 3), damage * 2, resolveProjectileEffect(tags)));
      default:
        return new PlantFood(
                1,
                new DummyPlantAction(
                        "Plant Food effect '" + template.plantFoodEffect + "' not implemented"));
    }
  }
}