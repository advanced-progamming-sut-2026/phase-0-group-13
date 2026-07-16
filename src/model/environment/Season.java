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
      for (String keyword : keywords) {
        if (matchesAliasSegment(alias, keyword)) {
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
  
  private boolean matchesAliasSegment(String alias, String keyword) {
    String lower = alias.toLowerCase();
    String keywordLower = keyword.toLowerCase();
    int idx = 0;
    while ((idx = lower.indexOf(keywordLower, idx)) != -1) {
      boolean startsOnBoundary =
              idx == 0 || Character.isUpperCase(alias.charAt(idx)) || !Character.isLetter(alias.charAt(idx - 1));
      int endIdx = idx + keywordLower.length();
      boolean endsOnBoundary =
              endIdx == alias.length()
                      || Character.isUpperCase(alias.charAt(endIdx))
                      || !Character.isLetter(alias.charAt(endIdx));

      if (startsOnBoundary && endsOnBoundary) {
        return true;
      }
      idx++;
    }
    return false;
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