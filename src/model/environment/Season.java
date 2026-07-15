package model.environment;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.game.GameState;
import model.game.Tile;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.factory.ZombieFactory;
import view.MainMenuSubMenus.GameMenuSubMenus.MiniGames.MiniGame;

public abstract class Season {
  protected static final int DEFAULT_ROWS = 5;
  protected static final int DEFAULT_COLUMNS = 9;

  protected String name;
  private List<Zombie> availableZombies;
  private List<Stage> stages;
  private List<MiniGame> miniGames;

  public String getName() {
    return name;
  }

  public void initialize() {
    // داخل اینجا مینی گیم ها و .... اینیشالایز میکنیم
  }

  public abstract void applySeasonEffects(GameState gameState);

  public abstract List<Zombie> getAvailableZombies();

  public abstract List<Tile> generateMap();

  // چون Zombies.json فیلد جدا برای فصل/چپتر نداره، از روی alias خام زامبی‌ها فیلتر میکنیم (مثلا
  // "ZombieEgyptImpDefault" یا "ZombieIceAgeDodo")؛ همون قراردادی که ZombieTypeResolver هم برای
  // تشخیص نوع زامبی از روی اسم استفاده میکنه
  protected List<Zombie> zombiesByAliasKeyword(String... keywords) {
    List<Zombie> result = new ArrayList<>();
    if (GameDataManager.zombieRepository == null) {
      return result;
    }

    ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      String alias = template.getName();
      if (alias == null) {
        continue;
      }
      String lower = alias.toLowerCase();
      for (String keyword : keywords) {
        if (lower.contains(keyword)) {
          Zombie zombie = factory.createZombie(template.getName(), 0, 9.0);
          if (zombie != null) {
            result.add(zombie);
          }
          break;
        }
      }
    }
    return result;
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
