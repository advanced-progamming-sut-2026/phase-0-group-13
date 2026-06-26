package model.game.zombie.factory;

import data.repository.ZombieRepository;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.behavior.*;

public class ZombieFactory {
  private ZombieRepository repository;

  public ZombieFactory(ZombieRepository repository) {
    this.repository = repository;
  }

  public Zombie createZombie(String name, int row, double startX) {
    // جستجوی قالب زامبی در مخزن دیتامنیجر
    ZombieTemplate template = this.repository.find(name);

    if (template == null) {
      System.err.println("قالب زامبی یافت نشد: " + name);
      return null;
    }

    ZombieAction behavior = determineBehavior(template);

    Zombie zombie =
        new Zombie(template.name, template.baseHp, template.baseSpeed, row, startX, behavior);
    applyArmorLayers(zombie, template);

    return zombie;
  }

  private ZombieAction determineBehavior(ZombieTemplate template) {
    String name = template.name != null ? template.name.toLowerCase().trim() : "";
    String type = template.type != null ? template.type.toLowerCase().trim() : "";

    if (type.contains("zombotany") || name.contains("peashooter")) {
      return new ZombotanyPeashooterAction(150, 10.0); // شلیک هر 150 تیک
    }

    switch (name) {
      case "gargantuar":
        return new GargantuarAction(template.baseHp); // غول‌پیکر با قابلیت پرتا
      default:
        double eatingDamage = 10.0;
        if (template.damage != null && !template.damage.trim().isEmpty()) {
          try {
            eatingDamage = Double.parseDouble(template.damage.trim());
          } catch (NumberFormatException e) {
          }
        }
        return new StandardZombieAction(eatingDamage);
    }
  }

  private void applyArmorLayers(Zombie zombie, ZombieTemplate template) {
    String name = template.name != null ? template.name.toLowerCase().trim() : "";

    if (template.armorHp > 0) {
      boolean isMetallic = name.equals("buckethead") || name.equals("knight");
      zombie.addArmor(new Armor(template.name + " Armor", template.armorHp, isMetallic));
    } else if (name.equals("knight")) {
      zombie.addArmor(new Armor("Helmet", 1600, true)); // لایه رویی
      zombie.addArmor(new Armor("Shoulder Armor", 1600, true)); // لایه زیری
    }
  }

  public Zombie createZombie(String name, int row, double startX, double extraParam) {

    return createZombie(name, row, startX);
  }
}
