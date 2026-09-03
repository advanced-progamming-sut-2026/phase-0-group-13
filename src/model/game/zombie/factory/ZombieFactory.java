package model.game.zombie.factory;

import data.repository.ZombieRepository;
import java.util.List;
import java.util.Random;
import model.core.Difficulty;
import model.enums.ZombieType;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.behavior.*;

public class ZombieFactory {
  /** The All-Star crawls after it flattens its first plant. */
  private static final double FOOTBALLER_SLOWDOWN = 0.35;

  private static final double SHINY_CHANCE = 0.05;
  private static final double TICKS_PER_SECOND = 10.0;

  private final ZombieRepository repository;
  private final Random random = new Random();

  public ZombieFactory(ZombieRepository repository) {
    this.repository = repository;
  }

  public Zombie createZombie(String name, int row, double startX) {
    ZombieTemplate template = this.repository.find(name);
    if (template == null) {
      System.err.println("Zombie template not found: " + name);
      return null;
    }

    ZombieType type = ZombieTypeResolver.resolve(template);
    int scaledHp = (int) Math.round(resolveBaseHp(template, type) * Difficulty.zombieHealth());
    ZombieAction behavior = determineBehavior(template, type, Difficulty.zombieDamage(), scaledHp);

    Zombie zombie = new Zombie(
            template.getName(),
            Math.max(1, scaledHp),
            template.getBaseSpeed() * Difficulty.zombieSpeed() / TICKS_PER_SECOND,
            row, startX, behavior
    );

    zombie.setDisplayName(type.getDisplayName());
    zombie.setFireImmune(type == ZombieType.IMP_DRAGON);
    if (isZomboss(type)) {
      zombie.setBoss(true);
      zombie.setRowSpan(ZombossAction.ROW_SPAN);
    }
    applyArmorLayers(zombie, template);
    applyLootDrops(zombie);

    return zombie;
  }

  private int resolveBaseHp(ZombieTemplate template, ZombieType type) {
    int hp = template.getBaseHp();
    if (hp <= 0 && isZomboss(type)) return 5000;
    return hp;
  }

  private boolean isZomboss(ZombieType type) {
    return type == ZombieType.ZOMBOSS_EGYPT || type == ZombieType.ZOMBOSS_PIRATE ||
            type == ZombieType.ZOMBOSS_COWBOY || type == ZombieType.ZOMBOSS_DARK;
  }

  private void applyLootDrops(Zombie zombie) {
    if (random.nextDouble() < SHINY_CHANCE) zombie.setShiny(true);
  }

  private ZombieAction determineBehavior(
          ZombieTemplate template, ZombieType type, double diffMultiplier, int scaledHp) {
    double eatDamage = template.getEatDps() * diffMultiplier;
    return switch (type) {
      case NORMAL, CONEHEAD, BUCKETHEAD, KNIGHT, BLOCKHEAD, IMP,
           ZOMBOTANY_WALLNUT -> new StandardZombieAction(eatDamage);
      case ZOMBOSS_EGYPT, ZOMBOSS_PIRATE, ZOMBOSS_COWBOY, ZOMBOSS_DARK ->
              new ZombossAction(type,
                      new ZombossHealth(template.getStageHitPoints(), Math.max(1, scaledHp)),
                      eatDamage);
      case TURQUOISE -> new TurquoiseZombieAction(50, 10);
      case PIANIST -> new PianistZombieAction(60);
      case BARREL_ROLLER -> new BarrelRollerZombieAction(eatDamage);
      case GARGANTUAR -> new GargantuarAction((int) Math.round(resolveBaseHp(template, type) * diffMultiplier));
      case EXPLORER -> new ExplorerZombieAction();
      case ZOMBOTANY_PEASHOOTER -> new ZombotanyPeashooterAction(15, eatDamage);
      case HUNTER -> new HunterZombieAction(60, eatDamage);
      case ZOMBOTANY_JALAPENO -> new ZombotanyJalapenoAction(100);
      case ZOMBOTANY_SQUASH -> new TacklerZombieAction(true);
      case IMP_DRAGON -> new StandardZombieAction(eatDamage);
      case FOOTBALLER -> new TacklerZombieAction(false, FOOTBALLER_SLOWDOWN);
      case ARCADE -> new TacklerZombieAction();
      case PARASOL -> new ParasolZombieAction(200, eatDamage);
      case PROSPECTOR -> new ProspectorZombieAction(100, eatDamage);
      case NEWSPAPER -> new EnrageOnArmorBreakZombieAction(eatDamage, 2.0);
      case RA -> new RaHealAuraZombieAction(100, 50, 2.0, eatDamage);
      case TOMBRAISER -> new TombRaiserZombieAction(300, eatDamage);
      case DODO_RIDER -> new DodoRiderZombieAction(eatDamage);
      case JUGGLER -> new JesterZombieAction(eatDamage);
      case TROGLOBITE -> new IceBlockPusherZombieAction(600, eatDamage);
      case FISHERMAN -> new HookPullZombieAction(70, eatDamage);
      case OCTOPUS -> new OctopusThrowerZombieAction(70, eatDamage);
      case SNORKEL -> new SubmergedZombieAction(50, 100, eatDamage);
      case WIZARD -> new WizardZombieAction(eatDamage);
      case KING -> new KingAuraZombieAction(1.5, 3.0, eatDamage);
      default -> throw new UnsupportedOperationException("Unknown ZombieType: " + type);
    };
  }

  private void applyArmorLayers(Zombie zombie, ZombieTemplate template) {
    List<Integer> armorHps = repository.resolveArmorHp(template);
    List<model.enums.ArmorType> armorTypes = repository.resolveArmorTypes(template);
    String name = template.getName() == null ? "" : template.getName().toLowerCase();
    boolean metallic = name.contains("bucket") || name.contains("knight") || name.contains("crown");

    for (int i = 0; i < armorHps.size(); i++) {
      model.enums.ArmorType type = i < armorTypes.size() ? armorTypes.get(i) : null;
      boolean layerMetallic = metallic || type == model.enums.ArmorType.BUCKET || type == model.enums.ArmorType.HELMET;
      zombie.addArmor(new Armor(template.getName() + " Armor", armorHps.get(i), layerMetallic, type));
    }
  }
}
