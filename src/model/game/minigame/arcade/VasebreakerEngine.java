package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public final class VasebreakerEngine {

  public static final int ROWS = 5;
  public static final int COLS = 9;
  private static final int SEED_TIMEOUT_TICKS = 100; // 10 seconds at 10 ticks/second

  public enum VaseContent {
    EMPTY,
    ZOMBIE,
    SEED_PACKET,
    PLANT_VASE,
    GARGANTUAR_VASE
  }
  public static final class ArcadeZombie {
    private final int row;
    private double positionCol;
    private int health;
    private final int damagePerHit;
    private final boolean isGargantuar;

    private ArcadeZombie(int row, double positionCol, int health, int damagePerHit, boolean isGargantuar) {
      this.row = row;
      this.positionCol = positionCol;
      this.health = health;
      this.damagePerHit = damagePerHit;
      this.isGargantuar = isGargantuar;
    }

    public int getRow() {
      return row;
    }

    public int getColumn() {
      return (int) Math.round(positionCol);
    }

    public int getHealth() {
      return health;
    }

    public boolean isGargantuar() {
      return isGargantuar;
    }

    public boolean isDead() {
      return health <= 0;
    }
  }

  private static final class ArcadePlant {
    private final int row;
    private final int col;
    private int health;
    private final int damagePerTick;

    private ArcadePlant(int row, int col, int health, int damagePerTick) {
      this.row = row;
      this.col = col;
      this.health = health;
      this.damagePerTick = damagePerTick;
    }

    private boolean isDead() {
      return health <= 0;
    }
  }

  private static final class PendingSeed {
    private final String plantName;
    private int ticksLeft;

    private PendingSeed(String plantName, int ticksLeft) {
      this.plantName = plantName;
      this.ticksLeft = ticksLeft;
    }
  }

  private final VaseContent[][] vaseGrid = new VaseContent[ROWS][COLS];
  private final boolean[][] smashed = new boolean[ROWS][COLS];
  private final List<ArcadeZombie> zombies = new ArrayList<>();
  private final List<ArcadePlant> plants = new ArrayList<>();
  private final List<PendingSeed> pendingSeeds = new ArrayList<>();
  private final Random random;
  private final int level;

  private boolean won;
  private boolean lost;
  private int tickCount;

  public VasebreakerEngine(int level) {
    this(level, new Random());
  }

  public VasebreakerEngine(int level, Random random) {
    this.level = level;
    this.random = random;
    generateVases();
  }

  private void generateVases() {

    int zombieVaseCount = 4 + level * 2;
    int seedVaseCount = 3 + level;
    int plantVaseCount = 2;
    boolean allowGargantuar = level >= 3;

    List<int[]> allTiles = new ArrayList<>();
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        allTiles.add(new int[] {r, c});
        vaseGrid[r][c] = VaseContent.EMPTY;
      }
    }
    java.util.Collections.shuffle(allTiles, random);

    int index = 0;
    if (allowGargantuar) {
      int[] tile = allTiles.get(index++);
      vaseGrid[tile[0]][tile[1]] = VaseContent.GARGANTUAR_VASE;
    }
    for (int i = 0; i < zombieVaseCount && index < allTiles.size(); i++, index++) {
      int[] tile = allTiles.get(index);
      vaseGrid[tile[0]][tile[1]] = VaseContent.ZOMBIE;
    }
    for (int i = 0; i < seedVaseCount && index < allTiles.size(); i++, index++) {
      int[] tile = allTiles.get(index);
      vaseGrid[tile[0]][tile[1]] = VaseContent.SEED_PACKET;
    }
    for (int i = 0; i < plantVaseCount && index < allTiles.size(); i++, index++) {
      int[] tile = allTiles.get(index);
      vaseGrid[tile[0]][tile[1]] = VaseContent.PLANT_VASE;
    }
  }

  public String smash(int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    if (smashed[row][col]) {
      return "error: that vase is already smashed";
    }
    smashed[row][col] = true;
    VaseContent content = vaseGrid[row][col];

    switch (content) {
      case EMPTY:
        return "Just an empty vase. Dust everywhere.";
      case ZOMBIE: {
        int health = 90 + level * 40;
        zombies.add(new ArcadeZombie(row, COLS - 1, health, 6, false));
        return "A zombie was hiding in the vase! It's loose on row " + (row + 1) + ".";
      }
      case GARGANTUAR_VASE: {
        int health = 1400 + level * 200;
        zombies.add(new ArcadeZombie(row, COLS - 1, health, 40, true));
        return "A GARGANTUAR smashes out of the vase on row " + (row + 1) + "! Run!";
      }
      case SEED_PACKET: {
        String plant = randomStarterPlant();
        pendingSeeds.add(new PendingSeed(plant, SEED_TIMEOUT_TICKS));
        return "Found a seed packet: " + plant + "! Plant it before it wilts.";
      }
      case PLANT_VASE: {
        int health = 250 + level * 50;
        int dmg = 18 + level * 4;
        plants.add(new ArcadePlant(row, col, health, dmg));
        return "A pre-grown defensive plant tumbles out and takes root at (" + (col + 1)
            + ", " + (row + 1) + ")!";
      }
      default:
        return "Nothing happens.";
    }
  }

  private String randomStarterPlant() {
    String[] pool = {"peashooter", "wall-nut", "cabbage-pult", "puff-shroom", "potato-mine"};
    return pool[random.nextInt(pool.length)];
  }

  public String plantSeed(String plantName, int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    PendingSeed match = null;
    for (PendingSeed seed : pendingSeeds) {
      if (seed.plantName.equalsIgnoreCase(plantName)) {
        match = seed;
        break;
      }
    }
    if (match == null) {
      return "error: no fresh " + plantName + " seed packet available";
    }
    for (ArcadePlant p : plants) {
      if (p.row == row && p.col == col) {
        return "error: that tile is already occupied";
      }
    }
    pendingSeeds.remove(match);
    plants.add(new ArcadePlant(row, col, 200 + level * 40, 15 + level * 3));
    return plantName + " planted at (" + (col + 1) + ", " + (row + 1) + ").";
  }
  public void tick() {
    if (won || lost) {
      return;
    }
    tickCount++;
    pendingSeeds.removeIf(seed -> --seed.ticksLeft <= 0);
    for (ArcadePlant plant : plants) {
      if (plant.isDead()) {
        continue;
      }
      for (ArcadeZombie zombie : zombies) {
        if (zombie.row == plant.row && !zombie.isDead() && zombie.getColumn() >= plant.col) {
          zombie.health -= plant.damagePerTick;
          break;
        }
      }
    }
    for (ArcadeZombie zombie : zombies) {
      if (zombie.isDead()) {
        continue;
      }
      ArcadePlant blocker = null;
      for (ArcadePlant plant : plants) {
        if (!plant.isDead() && plant.row == zombie.row && plant.col == zombie.getColumn()) {
          blocker = plant;
          break;
        }
      }
      if (blocker != null) {
        blocker.health -= zombie.damagePerHit;
      } else {
        zombie.positionCol -= zombie.isGargantuar ? 0.05 : 0.1;
        if (zombie.positionCol <= 0) {
          lost = true;
          return;
        }
      }
    }

    plants.removeIf(ArcadePlant::isDead);
    zombies.removeIf(ArcadeZombie::isDead);

    boolean allVasesResolved = allZombieVasesSmashed();
    if (allVasesResolved && zombies.isEmpty()) {
      won = true;
    }
  }

  private boolean allZombieVasesSmashed() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        VaseContent content = vaseGrid[r][c];
        boolean dangerous = content == VaseContent.ZOMBIE || content == VaseContent.GARGANTUAR_VASE;
        if (dangerous && !smashed[r][c]) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isWon() {
    return won;
  }

  public boolean isLost() {
    return lost;
  }

  public boolean isFinished() {
    return won || lost;
  }

  public int getTickCount() {
    return tickCount;
  }

  public List<ArcadeZombie> getZombies() {
    return zombies;
  }

  public List<String> getPendingSeedNames() {
    List<String> names = new ArrayList<>();
    for (PendingSeed seed : pendingSeeds) {
      names.add(seed.plantName + " (" + (seed.ticksLeft / 10) + "s left)");
    }
    return names;
  }

  public VaseContent[][] getVaseGrid() {
    return vaseGrid;
  }

  public boolean[][] getSmashedGrid() {
    return smashed;
  }

  public String renderMap() {
    StringBuilder sb = new StringBuilder();
    sb.append("--- Vasebreaker: Level ").append(level).append(" ---\n");
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        char glyph;
        if (!smashed[r][c]) {
          glyph = 'V';
        } else {
          boolean hasZombie = false;
          for (ArcadeZombie z : zombies) {
            if (!z.isDead() && z.row == r && z.getColumn() == c) {
              hasZombie = true;
              break;
            }
          }
          glyph = hasZombie ? 'Z' : '.';
        }
        sb.append('[').append(glyph).append(']');
      }
      sb.append('\n');
    }
    if (!pendingSeeds.isEmpty()) {
      sb.append("Fresh seeds: ").append(getPendingSeedNames()).append('\n');
    }
    return sb.toString();
  }
}