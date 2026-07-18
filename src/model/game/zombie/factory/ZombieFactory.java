package model.game.zombie.factory;

import data.repository.ZombieRepository;
import java.util.List;
import java.util.Random;
import model.enums.ZombieType;
import model.game.reward.Currency;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.behavior.*;

public class ZombieFactory {
  private static final double SHINY_CHANCE = 0.05;

  private final ZombieRepository repository;
  private final Random random = new Random();

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
            new Zombie(
                    template.getName(),
                    resolveBaseHp(template, type),
                    template.getBaseSpeed(),
                    row,
                    startX,
                    behavior);
    applyArmorLayers(zombie, template);
    applyLootDrops(zombie);

    return zombie;
  }

  // Zombies.json برای ۴ تا Zomboss مقدار Hitpoints نداره (null -> getBaseHp() صفر برمیگردونه)، یعنی
  // بدون این فالبک همون لحظه اسپاون "مرده" حساب میشدن. عدد ۵۰۰۰ قراردادیه تا وقتی دیتای واقعی باس‌ها
  // اضافه بشه.
  private int resolveBaseHp(ZombieTemplate template, ZombieType type) {
    int hp = template.getBaseHp();
    if (hp <= 0 && isZomboss(type)) {
      return 5000;
    }
    return hp;
  }

  private boolean isZomboss(ZombieType type) {
    return type == ZombieType.ZOMBOSS_EGYPT
            || type == ZombieType.ZOMBOSS_PIRATE
            || type == ZombieType.ZOMBOSS_COWBOY
            || type == ZombieType.ZOMBOSS_DARK;
  }

  // دراپ آیتم: هر زامبی یه مقدار پایه سکه میده، و با شانس کم "درخشان" (shiny) میشه که یه پاداش الماس
  // اضافه هم بهش اضافه میکنه. اعمال واقعی این جایزه‌ها به یوزر هنوز جایی صدا زده نمیشه - باید موقع
  // مرگ زامبی تو Board/GameManager چک بشه.
  private void applyLootDrops(Zombie zombie) {
    zombie.addLoot(new Currency("COIN", 5));

    if (random.nextDouble() < SHINY_CHANCE) {
      zombie.setShiny(true);
      zombie.addLoot(new Currency("DIAMOND", 1));
    }
  }

  public Zombie createZombie(String name, int row, double startX, double extraParam) {
    return createZombie(name, row, startX);
  }


  // این بقیه ش باید کامل بشه
  // ارش تو باید بزنی فک کنم
  //
  // همه‌ی انواع الان یه رفتار واقعی دارن. برای اونایی که مکانیک اصلیشون (شنا کردن واقعی زیر آب،
  // چندفازی بودن باس‌ها) نیاز به قابلیتی داشت که Board/GameManager هنوز نداره، از نزدیک‌ترین مکانیک
  // ممکن با ابزارهای فعلی استفاده شده و محدودیتش تو کامنت خود کلاس رفتار نوشته شده (مثلا
  // SubmergedZombieAction، یا استفاده مجدد از رفتارهای مشابه مثل TacklerZombieAction/
  // ZombotanyPeashooterAction/RangedDemolisherZombieAction برای زامبی‌هایی که مکانیک واقعیشون تقریبا
  // یکیه).
  private ZombieAction determineBehavior(ZombieTemplate template, ZombieType type) {
    double eatDamage = template.getEatDps();
    switch (type) {
      case NORMAL:
      case CONEHEAD:
      case BUCKETHEAD:
      case KNIGHT:
      case BLOCKHEAD:
      case IMP:
      case ARCADE:
      case TURQUOISE:
      case PIANIST:
        return new StandardZombieAction(eatDamage);
      case GARGANTUAR:
        return new GargantuarAction(template.getBaseHp());
      case EXPLORER:
        return new ExplorerZombieAction();
      case ZOMBOTANY_PEASHOOTER:
        return new ZombotanyPeashooterAction(150, 10.0);
      case ZOMBOTANY_WALLNUT:
        return new StandardZombieAction(eatDamage);
      case ZOMBOTANY_JALAPENO:
      case ZOMBOTANY_SQUASH:
        return new TacklerZombieAction();
      case FOOTBALLER:
        return new TacklerZombieAction();
      case PARASOL:
        // parasolHp عدد قراردادیه، دیتای واقعی چتر تو Zombies.json نیست
        return new ParasolZombieAction(200, eatDamage);
      case PROSPECTOR:
        return new RangedDemolisherZombieAction(60, 3.0);
      case NEWSPAPER:
        return new EnrageOnArmorBreakZombieAction(eatDamage, 2.0);
      // زامبی‌هایی که از رو گیاه رد میشن و نابودش میکنن بدون توقف (همون مکانیک Footballer)
      case BARREL_ROLLER:
      case IMP_DRAGON:
        return new TacklerZombieAction();
      case RA:
        return new RaHealAuraZombieAction(100, 50, 2.0, eatDamage);
      case TOMBRAISER:
        return new TombRaiserZombieAction(300, eatDamage);

      case DODO_RIDER:
        return new DodoRiderZombieAction(eatDamage);
      // زامبی‌هایی که از فاصله به گیاه شلیک میکنن (همون مکانیک زامبوتانی نخودی)
      case HUNTER:
        return new ZombotanyPeashooterAction(120, eatDamage);
      case JUGGLER:
        return new JesterZombieAction(eatDamage);
      // زامبی‌هایی که از فاصله گیاه رو منفجر/نابود میکنن (همون مکانیک پراسپکتور)
      case TROGLOBITE:
        return new RangedDemolisherZombieAction(80, 2.0);
      case FISHERMAN:
      case OCTOPUS:
        return new HookPullZombieAction(70, eatDamage);
      case SNORKEL:
        return new SubmergedZombieAction(50, 100, eatDamage);
      case WIZARD:
        return new WizardZombieAction(150);
      case KING:
        return new KingAuraZombieAction(1.5, 3.0, eatDamage);
      // باس‌ها (Zomboss) فعلا با یه رفتار سنگین ولی عادی اسپاون میشن؛ مکانیک واقعی‌شون (چندفازی بودن،
      // پرتاب موشک/مین، اسپاون مینیون‌های ویژه در طول نبرد) یه انکانتر خاص در سطح GameManager هستش،
      // نه چیزی که با یه ZombieAction تک‌تیکی قابل نمایش باشه
      case ZOMBOSS_EGYPT:
      case ZOMBOSS_PIRATE:
      case ZOMBOSS_COWBOY:
      case ZOMBOSS_DARK:
        return new StandardZombieAction(eatDamage);
      default:
        throw new UnsupportedOperationException("Unknown ZombieType: " + type);
    }
  }

  private void applyArmorLayers(Zombie zombie, ZombieTemplate template) {
    List<Integer> armorHps = repository.resolveArmorHp(template);
    List<model.enums.ArmorType> armorTypes = repository.resolveArmorTypes(template);
    String n = template.getName() == null ? "" : template.getName().toLowerCase();
    boolean metallic = n.contains("bucket") || n.contains("knight") || n.contains("crown");

    for (int i = 0; i < armorHps.size(); i++) {
      model.enums.ArmorType type = i < armorTypes.size() ? armorTypes.get(i) : null;
      boolean layerMetallic = metallic || type == model.enums.ArmorType.BUCKET || type == model.enums.ArmorType.HELMET;
      zombie.addArmor(new Armor(template.getName() + " Armor", armorHps.get(i), layerMetallic, type));
    }
  }
}