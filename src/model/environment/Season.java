package model.environment;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.enums.ZombieType;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.factory.ZombieFactory;

public abstract class Season {
  protected static final int DEFAULT_ROWS = 5;
  protected static final int DEFAULT_COLUMNS = 9;

  protected String name;

  public String getName() {
    return name;
  }

  public void initialize() {
    // داخل اینجا مینی گیم ها و .... اینیشالایز میکنیم
  }

  public abstract void applySeasonEffects(GameState gameState);

  public abstract List<Zombie> getAvailableZombies();

  public abstract List<Tile> generateMap();

  // پیش‌فرض هیچ خطری رو نقشه نمیزاره؛ فصل‌هایی که نیاز دارن (سنگ‌قبر، یخ) این رو override میکنن.
  // بعد از initializeLevel صدا زده میشه (MatchLauncher)
  public void placeHazards(model.game.Board board) {}


  public String getBossZombieName() {
    return null;
  }

  public void onTick(model.game.Board board, int currentTick) {}

  public void onWaveStart(model.game.Board board, int waveNumber, int currentTick) {}

  /** نسخه‌ی کامل‌تر: می‌داند این موج، موج آخر (پرچم) است یا نه. */
  public void onWaveStart(
          model.game.Board board, int waveNumber, int currentTick, boolean finalWave) {
    onWaveStart(board, waveNumber, currentTick);
  }

  // فقط در موج آخر و هر تیک صدا زده می‌شود؛ فصل‌هایی مثل مصر باستان که ورود ویژه (گردباد) دارند
  // این را override می‌کنند تا زامبی‌های تازه‌وارد را همان لحظهٔ ورود جابه‌جا کنند
  public void onFinalWaveTick(model.game.Board board, int currentTick) {}

  /**
   * The zombies the spec lists under "common between all chapters" -- every chapter draws from
   * these on top of its own. Kept as types rather than aliases because the type is what the spec
   * names and what {@link ZombieFactory} gives a behaviour to; the alias is just how one
   * particular art set happens to be filed.
   */
  protected static final ZombieType[] COMMON_ZOMBIES = {
    ZombieType.NORMAL,
    ZombieType.CONEHEAD,
    ZombieType.BUCKETHEAD,
    ZombieType.KNIGHT,
    ZombieType.BLOCKHEAD,
    ZombieType.GARGANTUAR,
    ZombieType.IMP,
    ZombieType.FOOTBALLER,
    ZombieType.ARCADE,
    ZombieType.PARASOL,
    ZombieType.TURQUOISE,
    ZombieType.PROSPECTOR,
    ZombieType.PIANIST,
    ZombieType.NEWSPAPER,
    ZombieType.BARREL_ROLLER,
  };

  /**
   * This chapter's spawn pool: the common zombies plus the ones only it gets.
   *
   * <p>This used to filter Zombies.json by a keyword in the raw alias ("iceage", "beach", ...).
   * That read the art's world token rather than the roster, so a chapter got whichever zombies
   * upstream happened to file under its world name: Frostbite Caves and Big Wave Beach ended up
   * with three zombies each and no basic walker at all, while All-Star, Arcade, Newspaper,
   * Prospector and Parasol never spawned anywhere. The roster is now stated outright, from the
   * spec's own two lists, so what a chapter spawns no longer depends on how the art is filed.
   */
  protected List<Zombie> rosterOf(ZombieType... seasonOnly) {
    List<Zombie> result = new ArrayList<>();
    if (GameDataManager.zombieRepository == null) {
      return result;
    }

    List<ZombieType> wanted = new ArrayList<>(List.of(COMMON_ZOMBIES));
    for (ZombieType type : seasonOnly) {
      if (!wanted.contains(type)) {
        wanted.add(type);
      }
    }

    ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
    for (ZombieType type : wanted) {
      ZombieTemplate template = templateFor(type);
      if (template == null) {
        // The roster asks for something Zombies.json has no sheet for. Say so rather than
        // quietly shipping a chapter that is short a zombie.
        System.err.printf("Season %s: no zombie template resolves to %s%n", name, type);
        continue;
      }
      Zombie zombie = factory.createZombie(template.getName(), 0, 9.0);
      if (zombie != null) {
        result.add(zombie);
      }
    }
    return result;
  }

  /**
   * The sheet that stands for a type. First match in file order wins, which matters for NORMAL:
   * several sheets fall through {@link ZombieTypeResolver} to it, and the plain mummy is the one
   * listed first and the one with art.
   */
  private static ZombieTemplate templateFor(ZombieType type) {
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      String alias = template.getName();
      // GridItem* entries share the file with the zombies but are scenery, not spawnables
      if (alias == null || !alias.startsWith("Zombie") || isBossTier(template)) {
        continue;
      }
      if (ZombieTypeResolver.resolve(template) == type) {
        return template;
      }
    }
    return null;
  }

  // باس‌ها (Zomboss) با اینکه ممکنه اسمشون با کلیدواژه فصل مطابقت داشته باشه (مثلا
  // "ZombieZombossMechEgypt" با "egypt")، نباید تو استخر موج‌های معمولی قرار بگیرن؛ اونا مخصوص یه
  // مرحله باس‌فایت جدا هستن، نه اسپاون رندوم وسط یه موج عادی
  private static boolean isBossTier(ZombieTemplate template) {
    ZombieType type = ZombieTypeResolver.resolve(template);
    return type == ZombieType.ZOMBOSS_EGYPT
            || type == ZombieType.ZOMBOSS_PIRATE
            || type == ZombieType.ZOMBOSS_COWBOY
            || type == ZombieType.ZOMBOSS_DARK;
  }

  // فعلا فقط یه شبکه‌ی صاف از خونه‌های معمولی میسازه؛ چون Board الان خودش تایل‌هاش رو مستقل میسازه
  // (این متود جایی صدا زده نمیشه)، تا وقتی وصل نشه صرفا از تهی برگردوندن جلوگیری میکنه
  protected List<Tile> plainGrid() {
    List<Tile> tiles = new ArrayList<>();
    for (int i = 0; i < DEFAULT_ROWS * DEFAULT_COLUMNS; i++) {
      tiles.add(new Tile());
    }
    return tiles;
  }
}