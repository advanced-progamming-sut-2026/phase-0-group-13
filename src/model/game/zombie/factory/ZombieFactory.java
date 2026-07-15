package model.game.zombie.factory;

import data.repository.ZombieRepository;
import java.util.List;
import model.enums.ZombieType;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.ZombieParts.ZombieTemplate;
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

    ZombieType type = resolveType(template);
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

  private ZombieType resolveType(ZombieTemplate template) {
    String n = template.getName() == null ? "" : template.getName().toLowerCase();

    if (n.contains("gargantuar")) return ZombieType.GARGANTUAR;
    if (n.contains("imp") && n.contains("dragon")) return ZombieType.IMP_DRAGON;
    if (n.contains("imp")) return ZombieType.IMP;
    if (n.contains("explorer")) return ZombieType.EXPLORER;
    if (n.contains("allstar")) return ZombieType.FOOTBALLER;
    if (n.contains("arcade")) return ZombieType.ARCADE;
    if (n.startsWith("zombiera")) return ZombieType.RA;
    if (n.contains("tombraiser")) return ZombieType.TOMBRAISER;
    if (n.contains("dodo")) return ZombieType.DODO_RIDER;
    if (n.contains("hunter")) return ZombieType.HUNTER;
    if (n.contains("troglobite")) return ZombieType.TROGLOBITE;
    if (n.contains("fisherman")) return ZombieType.FISHERMAN;
    if (n.contains("snorkel")) return ZombieType.SNORKEL;
    if (n.contains("octopus")) return ZombieType.OCTOPUS;
    if (n.contains("juggler") || n.contains("jester")) return ZombieType.JUGGLER;
    if (n.contains("wizard")) return ZombieType.WIZARD;
    if (n.contains("king")) return ZombieType.KING;
    if (n.contains("piano")) return ZombieType.PIANIST;
    if (n.contains("prospector")) return ZombieType.PROSPECTOR;
    if (n.contains("barrel")) return ZombieType.BARREL_ROLLER;
    if (n.contains("parasol")) return ZombieType.PARASOL;
    if (n.contains("turquoise")) return ZombieType.TURQUOISE;
    if (n.contains("newspaper")) return ZombieType.NEWSPAPER;

    if (n.contains("zombossmech")) {
      if (n.contains("egypt")) return ZombieType.ZOMBOSS_EGYPT;
      if (n.contains("pirate")) return ZombieType.ZOMBOSS_PIRATE;
      if (n.contains("cowboy")) return ZombieType.ZOMBOSS_COWBOY;
      if (n.contains("dark")) return ZombieType.ZOMBOSS_DARK;
    }

    if (n.contains("zombotany")) {
      if (n.contains("wallnut") || n.contains("wall-nut")) return ZombieType.ZOMBOTANY_WALLNUT;
      return ZombieType.ZOMBOTANY_PEASHOOTER;
    }

    // Armor-suffix conventions seen in Zombies.json (e.g. "...Armor1Default")
    if (n.contains("armor4") || n.contains("brick")) return ZombieType.BLOCKHEAD;
    if (n.contains("armor3") || n.contains("knight") || n.contains("crown")) return ZombieType.KNIGHT;
    if (n.contains("armor2") || n.contains("bucket")) return ZombieType.BUCKETHEAD;
    if (n.contains("armor1") || n.contains("cone")) return ZombieType.CONEHEAD;

    return ZombieType.NORMAL;
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