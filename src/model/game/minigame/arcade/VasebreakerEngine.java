package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public final class VasebreakerEngine {

  public static final int ROWS = 5;
  public static final int COLS = 9;
  private static final int SEED_TIMEOUT_TICKS = 100; // 10 seconds at 10 ticks/second

  public enum VaseContent {
    NONE,
    EMPTY,
    ZOMBIE,
    SEED_PACKET,
    PLANT_VASE,
    GARGANTUAR_VASE
  }
  public static final class ArcadeZombie {
    private final String name;
    private final int row;
    private double positionCol;
    private int health;
    private final int maxHealth;
    private final int damagePerHit;
    private final boolean isGargantuar;

    private ArcadeZombie(String name, int row, double positionCol, int health, int damagePerHit,
            boolean isGargantuar) {
      this.name = name;
      this.row = row;
      this.positionCol = positionCol;
      this.health = health;
      this.maxHealth = health;
      this.damagePerHit = damagePerHit;
      this.isGargantuar = isGargantuar;
    }

    public String getName() {
      return name;
    }

    public int getRow() {
      return row;
    }

    public int getColumn() {
      return (int) Math.round(positionCol);
    }

    public double getExactColumn() {
      return positionCol;
    }

    public int getHealth() {
      return health;
    }

    public int getMaxHealth() {
      return maxHealth;
    }

    public boolean isGargantuar() {
      return isGargantuar;
    }

    public boolean isDead() {
      return health <= 0;
    }
  }

  public static final class ArcadePlant {
    private final String name;
    private final int row;
    private final int col;
    private int health;
    private final int damagePerTick;

    private ArcadePlant(String name, int row, int col, int health, int damagePerTick) {
      this.name = name;
      this.row = row;
      this.col = col;
      this.health = health;
      this.damagePerTick = damagePerTick;
    }

    private boolean isDead() {
      return health <= 0;
    }

    public String getName() {
      return name;
    }

    public int getRow() {
      return row;
    }

    public int getCol() {
      return col;
    }

    public int getHealth() {
      return health;
    }
  }

  public static final class PendingSeed {
    private final String plantName;
    private final int row;
    private final int col;
    private int ticksLeft;

    private PendingSeed(String plantName, int row, int col, int ticksLeft) {
      this.plantName = plantName;
      this.row = row;
      this.col = col;
      this.ticksLeft = ticksLeft;
    }

    public String getPlantName() {
      return plantName;
    }

    public int getRow() {
      return row;
    }

    public int getCol() {
      return col;
    }

    public int getTicksLeft() {
      return ticksLeft;
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
    int seedVaseCount = 4 - level;
    int plantVaseCount = 2;
    int gargantuarVaseCount = level;
    int emptyVaseCount = 4;

    List<int[]> allTiles = new ArrayList<>();
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        allTiles.add(new int[] {r, c});
        vaseGrid[r][c] = VaseContent.NONE;
      }
    }
    java.util.Collections.shuffle(allTiles, random);

    int index = 0;
    index = fillTiles(allTiles, index, gargantuarVaseCount, VaseContent.GARGANTUAR_VASE);
    index = fillTiles(allTiles, index, zombieVaseCount, VaseContent.ZOMBIE);
    index = fillTiles(allTiles, index, seedVaseCount, VaseContent.SEED_PACKET);
    index = fillTiles(allTiles, index, plantVaseCount, VaseContent.PLANT_VASE);
    fillTiles(allTiles, index, emptyVaseCount, VaseContent.EMPTY);
  }

  private int fillTiles(List<int[]> tiles, int startIndex, int count, VaseContent content) {
    int index = startIndex;
    for (int i = 0; i < count && index < tiles.size(); i++, index++) {
      int[] tile = tiles.get(index);
      vaseGrid[tile[0]][tile[1]] = content;
    }
    return index;
  }

  public String smash(int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    if (vaseGrid[row][col] == VaseContent.NONE) {
      return "error: there is no vase at (" + (col + 1) + ", " + (row + 1) + ")";
    }
    if (smashed[row][col]) {
      return "error: that vase is already smashed";
    }
    smashed[row][col] = true;
    VaseContent content = vaseGrid[row][col];
    String broken = String.format("Vase at (%d, %d) is broken.", col + 1, row + 1);

    switch (content) {
      case EMPTY:
        return broken + " It was empty; dust everywhere.";
      case ZOMBIE: {
        int health = 90 + level * 40;
        zombies.add(new ArcadeZombie("Zombie", row, col, health, 6, false));
        return broken + String.format(" Zombie released at (%d, %d).", col + 1, row + 1);
      }
      case GARGANTUAR_VASE: {
        int health = 1400 + level * 200;
        zombies.add(new ArcadeZombie("Gargantuar", row, col, health, 40, true));
        return broken + String.format(" Gargantuar released at (%d, %d)! Run!", col + 1, row + 1);
      }
      case SEED_PACKET:
      case PLANT_VASE:
        return broken + " " + dropSeedPacket(row, col);
      default:
        return broken;
    }
  }

  private String dropSeedPacket(int row, int col) {
    String plant = randomStarterPlant();
    pendingSeeds.add(new PendingSeed(plant, row, col, SEED_TIMEOUT_TICKS));
    return String.format("A %s seed packet dropped at (%d, %d); it wilts in %d seconds.",
            plant, col + 1, row + 1, SEED_TIMEOUT_TICKS / 10);
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
    if (vaseGrid[row][col] != VaseContent.NONE && !smashed[row][col]) {
      return "error: there is still an unbroken vase there - smash it first";
    }
    for (ArcadePlant p : plants) {
      if (p.row == row && p.col == col) {
        return "error: that tile is already occupied";
      }
    }
    pendingSeeds.remove(match);
    plants.add(new ArcadePlant(match.plantName, row, col, 200 + level * 40, 15 + level * 3));
    return match.plantName + " planted at (" + (col + 1) + ", " + (row + 1) + ").";
  }
  public void tick() {
    if (won || lost) {
      return;
    }
    tickCount++;
    expireSeeds();
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
          System.out.println("The zombie ate your brain; LOSER!!!");
          return;
        }
      }
    }

    reportCasualties();
    plants.removeIf(ArcadePlant::isDead);
    zombies.removeIf(ArcadeZombie::isDead);

    if (allVasesSmashed() && zombies.isEmpty()) {
      won = true;
      System.out.println("Every vase on the lawn is broken and the lawn is clear. You win!");
    }
  }
  private void expireSeeds() {
    for (PendingSeed seed : new ArrayList<>(pendingSeeds)) {
      if (--seed.ticksLeft <= 0) {
        pendingSeeds.remove(seed);
        System.out.printf("The %s seed packet at (%d, %d) wilted away.%n",
                seed.plantName, seed.col + 1, seed.row + 1);}}}
  private void reportCasualties() {
    for (ArcadeZombie zombie : zombies) {
      if (zombie.isDead()) {
        System.out.printf("Zombie of type %s is dead at (%d, %d).%n",
                zombie.getName(), zombie.getColumn() + 1, zombie.row + 1);}}
    for (ArcadePlant plant : plants) {
      if (plant.isDead()) {
        System.out.printf("Plant %s at (%d, %d) is destroyed.%n",
                plant.name, plant.col + 1, plant.row + 1);}}}

  private boolean allVasesSmashed() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        if (vaseGrid[r][c] != VaseContent.NONE && !smashed[r][c]) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isWon() {
    return won;
  }



  public boolean isFinished() {
    return won || lost;
  }


  public List<ArcadeZombie> getZombies() {
    return zombies;
  }

  public boolean isLost() {
    return lost;
  }

  public List<ArcadePlant> getPlants() {
    return plants;
  }

  public List<PendingSeed> getPendingSeeds() {
    return pendingSeeds;
  }

  public List<String> getPendingSeedNames() {
    List<String> names = new ArrayList<>();
    for (PendingSeed seed : pendingSeeds) {
      names.add(seed.plantName + " (" + (seed.ticksLeft / 10) + "s left)");
    }
    return names;
  }

  public boolean hasPendingSeedAt(int row, int col) {
    for (PendingSeed seed : pendingSeeds) {
      if (seed.row == row && seed.col == col) {
        return true;}}
    return false;}

  public int getPlantHealthAt(int row, int col) {
    for (ArcadePlant plant : plants) {
      if (plant.row == row && plant.col == col && !plant.isDead()) {
        return plant.health;}}
    return -1;}
  public VaseContent[][] getVaseGrid() {
    return vaseGrid;
  }

  public boolean[][] getSmashedGrid() {
    return smashed;
  }

}
