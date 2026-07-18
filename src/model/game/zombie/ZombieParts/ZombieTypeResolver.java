package model.game.zombie.ZombieParts;

import model.enums.ZombieType;
import model.game.zombie.ZombieParts.ZombieTemplate;


public final class ZombieTypeResolver {

  private ZombieTypeResolver() {}

  public static ZombieType resolve(ZombieTemplate template) {
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
      if (n.contains("jalapeno")) return ZombieType.ZOMBOTANY_JALAPENO;
      if (n.contains("squash")) return ZombieType.ZOMBOTANY_SQUASH;
      return ZombieType.ZOMBOTANY_PEASHOOTER;
    }
    if (n.contains("armor4") || n.contains("brick")) return ZombieType.BLOCKHEAD;
    if (n.contains("armor3") || n.contains("knight") || n.contains("crown")) return ZombieType.KNIGHT;
    if (n.contains("armor2") || n.contains("bucket")) return ZombieType.BUCKETHEAD;
    if (n.contains("armor1") || n.contains("cone")) return ZombieType.CONEHEAD;

    return ZombieType.NORMAL;
  }
}