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
  }

  public abstract void applySeasonEffects(GameState gameState);

  public abstract List<Zombie> getAvailableZombies();

  public abstract List<Tile> generateMap();

  public void placeHazards(model.game.Board board) {}


  public String getBossZombieName() {
    return null;
  }

  public void onTick(model.game.Board board, int currentTick) {}

  public void onWaveStart(model.game.Board board, int waveNumber, int currentTick) {}

  public void onWaveStart(
          model.game.Board board, int waveNumber, int currentTick, boolean finalWave) {
    onWaveStart(board, waveNumber, currentTick);
  }

  public void onFinalWaveTick(model.game.Board board, int currentTick) {}

  /**
   * The zombies the spec lists under "common between all chapters" -- every chapter draws from
   * these on top of its own. Kept as types rather than aliases because the type is what the spec
   * names and what {@link ZombieFactory} gives a behaviour to; the alias is just how one particular
   * art set happens to be filed.
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

  private static ZombieTemplate templateFor(ZombieType type) {
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      String alias = template.getName();
      if (alias == null || !alias.startsWith("Zombie") || isBossTier(template)) {
        continue;
      }
      if (ZombieTypeResolver.resolve(template) == type) {
        return template;
      }
    }
    return null;
  }

  private static boolean isBossTier(ZombieTemplate template) {
    ZombieType type = ZombieTypeResolver.resolve(template);
    return type == ZombieType.ZOMBOSS_EGYPT
            || type == ZombieType.ZOMBOSS_PIRATE
            || type == ZombieType.ZOMBOSS_COWBOY
            || type == ZombieType.ZOMBOSS_DARK;
  }

  protected List<Tile> plainGrid() {
    List<Tile> tiles = new ArrayList<>();
    for (int i = 0; i < DEFAULT_ROWS * DEFAULT_COLUMNS; i++) {
      tiles.add(new Tile());
    }
    return tiles;
  }
}
