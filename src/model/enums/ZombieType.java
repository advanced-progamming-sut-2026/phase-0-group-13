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

  private final String displayName;

  ZombieType(String displayName) {
    this.displayName = displayName;
  }

  /** What the player is shown, instead of the raw asset alias. */
  public String getDisplayName() {
    return displayName;
  }
}
