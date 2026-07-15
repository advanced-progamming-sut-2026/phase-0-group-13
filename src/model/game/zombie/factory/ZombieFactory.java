package model.game.zombie.factory;

import data.repository.ZombieRepository;
import java.util.List;
import model.enums.ZombieType;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.behavior.*;

public class ZombieFactory {
  private final ZombieRepository repository;

  public ZombieFactory(ZombieRepository repository) {
    this.repository = repository;
  }

  public Zombie createZombie(String name, int row, double startX) {
    ZombieTemplate template = this.repository.find(name);

    if (template == null) {
      System.err.println("قالب زامبی یافت نشد: " + name);
      return null;
    }

    ZombieType type = ZombieTypeResolver.resolve(template);
    ZombieAction behavior = determineBehavior(template, type);

    Zombie zombie =
            new Zombie(template.getName(), template.getBaseHp(), template.getBaseSpeed(), row, startX, behavior);
    applyArmorLayers(zombie, template);

    return zombie;
  }

  public Zombie createZombie(String name, int row, double startX, double extraParam) {
    return createZombie(name, row, startX);
  }


  // این بقیه ش باید کامل بشه
  // ارش تو باید بزنی فک کنم
  private ZombieAction determineBehavior(ZombieTemplate template, ZombieType type) {
    double eatDamage = template.getEatDps();

    switch (type) {
      case NORMAL:
      case CONEHEAD:
      case BUCKETHEAD:
      case KNIGHT:
      case BLOCKHEAD:
        return new StandardZombieAction(eatDamage);
      case GARGANTUAR:
        return new GargantuarAction(template.getBaseHp());
      case EXPLORER:
        return new ExplorerZombieAction();
      case ZOMBOTANY_PEASHOOTER:
        return new ZombotanyPeashooterAction(150, 10.0);
      case ZOMBOTANY_WALLNUT:
        return new StandardZombieAction(eatDamage);
      case IMP:
      case FOOTBALLER:
      case ARCADE:
      case PARASOL:
      case TURQUOISE:
      case PROSPECTOR:
      case PIANIST:
      case NEWSPAPER:
      case BARREL_ROLLER:
      case RA:
      case TOMBRAISER:
      case DODO_RIDER:
      case HUNTER:
      case TROGLOBITE:
      case FISHERMAN:
      case SNORKEL:
      case OCTOPUS:
      case JUGGLER:
      case WIZARD:
      case KING:
      case IMP_DRAGON:
      case ZOMBOSS_EGYPT:
      case ZOMBOSS_PIRATE:
      case ZOMBOSS_COWBOY:
      case ZOMBOSS_DARK:
        return new DummyZombieAction(type, eatDamage);

      default:
        throw new UnsupportedOperationException("Unknown ZombieType: " + type);
    }
  }

  private void applyArmorLayers(Zombie zombie, ZombieTemplate template) {
    List<Integer> armorHps = repository.resolveArmorHp(template);
    String n = template.getName() == null ? "" : template.getName().toLowerCase();
    boolean metallic = n.contains("bucket") || n.contains("knight") || n.contains("crown");

    for (int hp : armorHps) {
      zombie.addArmor(new Armor(template.getName() + " Armor", hp, metallic));
    }
  }
}