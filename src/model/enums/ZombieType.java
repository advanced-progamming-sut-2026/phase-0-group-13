package model.enums;

public enum ZombieType {
  NORMAL("Basic"),
  CONEHEAD("ConeHead"),
  BUCKETHEAD("BucketHead"),
  KNIGHT("Knight"),
  BLOCKHEAD("BlockHead"),

  GARGANTUAR("Gargantuar"),
  IMP("Imp"),

  FOOTBALLER("All-Star"),
  ARCADE("Arcade"),
  PARASOL("Parasol"),
  TURQUOISE("Turquoise"),
  PROSPECTOR("Prospector"),
  PIANIST("Pianist"),
  NEWSPAPER("Newspaper"),
  BARREL_ROLLER("Barrel Roller"),

  RA("Ra"),
  EXPLORER("Explorer"),
  TOMBRAISER("Tomb Raiser"),

  DODO_RIDER("Dodo Rider"),
  HUNTER("Hunter"),
  TROGLOBITE("Troglobite"),

  FISHERMAN("Fisherman"),
  SNORKEL("Snorkel"),
  OCTOPUS("Octopus"),

  JUGGLER("Jester"),
  WIZARD("Wizard"),
  KING("King"),
  IMP_DRAGON("Imp Dragon"),

  ZOMBOTANY_PEASHOOTER("Zombotany Peashooter"),
  ZOMBOTANY_WALLNUT("Zombotany Wall-nut"),
  ZOMBOTANY_JALAPENO("Zombotany Jalapeno"),
  ZOMBOTANY_SQUASH("Zombotany Squash"),

  ZOMBOSS_EGYPT("Dr. Zomboss (Ancient Egypt)"),
  ZOMBOSS_PIRATE("Dr. Zomboss (Big Wave Beach)"),
  ZOMBOSS_COWBOY("Dr. Zomboss (Frostbite Caves)"),
  ZOMBOSS_DARK("Dr. Zomboss (Dark Ages)");

  /**
   * Whether this zombie can be on a water tile.
   *
   * <p>Only the sea roster and the bosses can: the Fisherman works from a boat, the Snorkel swims,
   * the Octopus rides the surface, and a Zomboss is a machine that hovers over the lawn. Everything
   * else is a land unit, and Big Wave Beach's whole right-hand side is sea.
   */
  public boolean canCrossWater() {
    return this == FISHERMAN || this == SNORKEL || this == OCTOPUS
            || this == ZOMBOSS_EGYPT || this == ZOMBOSS_PIRATE
            || this == ZOMBOSS_COWBOY || this == ZOMBOSS_DARK;
  }

  private final String displayName;

  ZombieType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
