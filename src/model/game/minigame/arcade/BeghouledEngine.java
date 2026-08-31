package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class BeghouledEngine {

  public static final int ROWS = 5;
  public static final int COLS = 9;
  public static final int MIN_MATCH = 3;
  public static final int SUN_PER_MATCH_UNIT = 50;

  private static final int MAX_CASCADE_CHAIN = 50;
  private static final int MAX_RESHUFFLE_ATTEMPTS = 50;

  public enum PlantKind {
    PEASHOOTER("peashooter", 'P'),
    SUNFLOWER("sunflower", 'S'),
    WALLNUT("wall-nut", 'W'),
    CABBAGE_PULT("cabbage-pult", 'C'),
    PUFF_SHROOM("puff-shroom", 'U'),

    REPEATER("repeater", 'R'),
    MEGA_GATLING_PEA("mega-gatling-pea", 'M'),
    TALL_NUT("tall-nut", 'T'),
    MELON_PULT("melon-pult", 'O'),
    WINTER_MELON("winter-melon", 'I'),
    FUME_SHROOM("fume-shroom", 'F');

    public final String label;
    public final char glyph;

    PlantKind(String label, char glyph) {
      this.label = label;
      this.glyph = glyph;
    }

    static PlantKind byLabel(String name) {
      if (name == null) {
        return null;
      }
      for (PlantKind kind : values()) {
        if (kind.label.equalsIgnoreCase(name.trim())) {
          return kind;
        }
      }
      return null;
    }
  }

  public static final class Upgrade {
    public final PlantKind from;
    public final PlantKind to;
    public final int cost;

    private Upgrade(PlantKind from, PlantKind to, int cost) {
      this.from = from;
      this.to = to;
      this.cost = cost;
    }
  }

  private static Map<PlantKind, Upgrade> buildUpgrades(int level) {
    Map<PlantKind, Upgrade> map = new LinkedHashMap<>();
    switch (Math.min(Math.max(level, 1), 3)) {
      case 1:
        map.put(PlantKind.PEASHOOTER, new Upgrade(PlantKind.PEASHOOTER, PlantKind.REPEATER, 500));
        map.put(PlantKind.REPEATER,
                new Upgrade(PlantKind.REPEATER, PlantKind.MEGA_GATLING_PEA, 1500));
        map.put(PlantKind.CABBAGE_PULT,
                new Upgrade(PlantKind.CABBAGE_PULT, PlantKind.MELON_PULT, 1000));
        map.put(PlantKind.MELON_PULT,
                new Upgrade(PlantKind.MELON_PULT, PlantKind.WINTER_MELON, 750));
        map.put(PlantKind.WALLNUT, new Upgrade(PlantKind.WALLNUT, PlantKind.TALL_NUT, 500));
        map.put(PlantKind.PUFF_SHROOM,
                new Upgrade(PlantKind.PUFF_SHROOM, PlantKind.FUME_SHROOM, 250));
        return map;
      case 2:
        map.put(PlantKind.PEASHOOTER, new Upgrade(PlantKind.PEASHOOTER, PlantKind.REPEATER, 600));
        map.put(PlantKind.REPEATER,
                new Upgrade(PlantKind.REPEATER, PlantKind.MEGA_GATLING_PEA, 1800));
        map.put(PlantKind.MELON_PULT,
                new Upgrade(PlantKind.MELON_PULT, PlantKind.WINTER_MELON, 900));
        map.put(PlantKind.WALLNUT, new Upgrade(PlantKind.WALLNUT, PlantKind.TALL_NUT, 600));
        map.put(PlantKind.PUFF_SHROOM,
                new Upgrade(PlantKind.PUFF_SHROOM, PlantKind.FUME_SHROOM, 300));
        return map;
      default:
        map.put(PlantKind.PEASHOOTER, new Upgrade(PlantKind.PEASHOOTER, PlantKind.REPEATER, 750));
        map.put(PlantKind.REPEATER,
                new Upgrade(PlantKind.REPEATER, PlantKind.MEGA_GATLING_PEA, 2000));
        map.put(PlantKind.MELON_PULT,
                new Upgrade(PlantKind.MELON_PULT, PlantKind.WINTER_MELON, 1000));
        map.put(PlantKind.WALLNUT, new Upgrade(PlantKind.WALLNUT, PlantKind.TALL_NUT, 700));
        return map;
    }
  }

  private static PlantKind[] startingKinds(int level) {
    switch (Math.min(Math.max(level, 1), 3)) {
      case 1:
        return new PlantKind[] {PlantKind.PEASHOOTER, PlantKind.SUNFLOWER, PlantKind.WALLNUT,
            PlantKind.CABBAGE_PULT, PlantKind.PUFF_SHROOM};
      case 2:
        return new PlantKind[] {PlantKind.PEASHOOTER, PlantKind.SUNFLOWER, PlantKind.WALLNUT,
            PlantKind.MELON_PULT, PlantKind.PUFF_SHROOM};
      default:
        return new PlantKind[] {PlantKind.PEASHOOTER, PlantKind.SUNFLOWER, PlantKind.WALLNUT,
            PlantKind.MELON_PULT, PlantKind.FUME_SHROOM};
    }
  }

  private static final class LaneZombie {
    private final int row;
    private double column;
    private int health;
    private int chewTicks;

    private LaneZombie(int row, double column, int health) {
      this.row = row;
      this.column = column;
      this.health = health;
    }

    private boolean isDead() {
      return health <= 0;
    }
  }

  private final Map<PlantKind, Upgrade> upgrades;
  private final PlantKind[] startingKinds;
  private final PlantKind[][] grid = new PlantKind[ROWS][COLS];
  private final boolean[][] crater = new boolean[ROWS][COLS];
  private final List<LaneZombie> zombies = new ArrayList<>();
  private final Random random;
  private final int level;
  private final int matchTarget;

  private int sun;
  private int matchesMade;
  private int ticksSinceSpawn;
  private int ticksSinceZombieAdvance;
  private final int ticksBetweenSpawns;
  private boolean won;
  private boolean lost;

  private static final int ZOMBIE_MOVE_INTERVAL_TICKS = 8;
  private static final int CHEW_TICKS = 24;

  public BeghouledEngine(int level) {
    this(level, new Random());
  }

  public BeghouledEngine(int level, Random random) {
    this.level = level;
    this.random = random;
    this.matchTarget = 6 + level * 3;
    this.ticksBetweenSpawns = Math.max(20, 70 - level * 12);
    this.upgrades = buildUpgrades(level);
    this.startingKinds = startingKinds(level);
    reshuffleBoard();
  }

  private void reshuffleBoard() {
    for (int attempt = 0; attempt < MAX_RESHUFFLE_ATTEMPTS; attempt++) {
      for (int row = 0; row < ROWS; row++) {
        for (int col = 0; col < COLS; col++) {
          grid[row][col] = crater[row][col] ? null : randomKind();
        }
      }
      if (findMatchGroups().isEmpty() && hasAnyValidMove()) {
        return;
      }
    }
  }

  private PlantKind randomKind() {
    return startingKinds[random.nextInt(startingKinds.length)];
  }

  public String swap(int rowA, int colA, int rowB, int colB) {
    if (won || lost) {
      return "error: this mini-game is already over.";
    }
    if (!inBounds(rowA, colA) || !inBounds(rowB, colB)) {
      return "error: coordinates out of bounds";
    }
    if (!isAdjacent(rowA, colA, rowB, colB)) {
      return "error: you can only swap two neighbouring tiles.";
    }
    if (grid[rowA][colA] == null || grid[rowB][colB] == null) {
      return "error: there is a crater there; nothing can be moved into it.";
    }

    swapCells(rowA, colA, rowB, colB);
    if (findMatchGroups().isEmpty()) {
      swapCells(rowA, colA, rowB, colB);
      return "error: that move doesn't create a match of " + MIN_MATCH + " or more.";
    }

    int gainedSun = resolveBoard();
    checkEndConditions();
    return "Swapped (" + (colA + 1) + ", " + (rowA + 1) + ") with (" + (colB + 1) + ", "
            + (rowB + 1) + "). Sun gained: " + gainedSun
            + " | matches: " + matchesMade + "/" + matchTarget + " | sun: " + sun;
  }

  public String upgrade(String fromLabel) {
    if (won || lost) {
      return "error: this mini-game is already over.";
    }
    PlantKind from = PlantKind.byLabel(fromLabel);
    if (from == null) {
      return "error: unknown plant '" + fromLabel + "'";
    }
    Upgrade upgrade = upgrades.get(from);
    if (upgrade == null) {
      return "error: " + from.label + " has no upgrade.";
    }
    if (sun < upgrade.cost) {
      return "error: not enough sun (need " + upgrade.cost + ", have " + sun + ")";
    }
    if (countOf(from) == 0) {
      return "error: there is no " + from.label + " on the lawn to upgrade.";
    }

    sun -= upgrade.cost;
    int converted = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        if (grid[row][col] == from) {
          grid[row][col] = upgrade.to;
          converted++;
        }
      }
    }

    int cascadeSun = resolveBoard();
    checkEndConditions();
    return "Upgraded " + converted + " " + from.label + " into " + upgrade.to.label + " for "
            + upgrade.cost + " sun (cascade bonus: " + cascadeSun + "). Sun left: " + sun;
  }

  private int countOf(PlantKind kind) {
    int count = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        if (grid[row][col] == kind) {
          count++;
        }
      }
    }
    return count;
  }

  private int resolveBoard() {
    int totalSun = 0;
    int chain = 0;

    List<List<int[]>> groups = findMatchGroups();
    while (!groups.isEmpty() && chain < MAX_CASCADE_CHAIN) {
      if (chain > 0) {
        System.out.printf("Cascade %d! The falling plants lined up %d more match(es).%n",
                chain, groups.size());
      }
      for (List<int[]> group : groups) {
        int sunUnits = group.size() - (MIN_MATCH - 1) + (chain > 0 ? 1 : 0);
        int gained = sunUnits * SUN_PER_MATCH_UNIT;
        totalSun += gained;
        matchesMade++;
        int[] head = group.get(0);
        System.out.printf("Match of %d %s starting at (%d, %d): +%d sun.%n",
                group.size(), grid[head[0]][head[1]].label, head[1] + 1, head[0] + 1, gained);
        if (group.size() >= MIN_MATCH + 1) {
          System.out.printf("Combo! A %d-in-a-row pays %d sun instead of %d.%n",
                  group.size(), gained, SUN_PER_MATCH_UNIT);
        }
      }
      for (List<int[]> group : groups) {
        for (int[] cell : group) {
          grid[cell[0]][cell[1]] = null;
        }
      }
      applyGravityAndRefill();
      chain++;
      groups = findMatchGroups();
    }

    sun += totalSun;
    ensureBoardIsPlayable();
    return totalSun;
  }

  private List<List<int[]>> findMatchGroups() {
    List<List<int[]>> groups = new ArrayList<>();
    for (int row = 0; row < ROWS; row++) {
      collectRuns(groups, row, 0, 0, 1);
    }
    for (int col = 0; col < COLS; col++) {
      collectRuns(groups, 0, col, 1, 0);
    }
    return groups;
  }

  private void collectRuns(
          List<List<int[]>> groups, int startRow, int startCol, int dRow, int dCol) {
    int row = startRow;
    int col = startCol;
    int runStartRow = startRow;
    int runStartCol = startCol;
    int runLength = 0;
    PlantKind previous = null;

    while (inBounds(row, col)) {
      PlantKind current = grid[row][col];
      if (current != null && current == previous) {
        runLength++;
      } else {
        addRun(groups, runStartRow, runStartCol, dRow, dCol, runLength);
        runStartRow = row;
        runStartCol = col;
        runLength = current == null ? 0 : 1;
      }
      previous = current;
      row += dRow;
      col += dCol;
    }
    addRun(groups, runStartRow, runStartCol, dRow, dCol, runLength);
  }

  private void addRun(
          List<List<int[]>> groups, int row, int col, int dRow, int dCol, int runLength) {
    if (runLength < MIN_MATCH) {
      return;
    }
    List<int[]> group = new ArrayList<>();
    for (int i = 0; i < runLength; i++) {
      group.add(new int[] {row + dRow * i, col + dCol * i});
    }
    groups.add(group);
  }

  private void applyGravityAndRefill() {
    for (int col = 0; col < COLS; col++) {
      int segmentBottom = ROWS - 1;
      for (int row = ROWS - 1; row >= -1; row--) {
        if (row < 0 || crater[row][col]) {
          compactSegment(col, row + 1, segmentBottom);
          segmentBottom = row - 1;
        }
      }
    }
  }

  private void compactSegment(int col, int topRow, int bottomRow) {
    if (topRow > bottomRow) {
      return;
    }
    int writeRow = bottomRow;
    for (int row = bottomRow; row >= topRow; row--) {
      if (grid[row][col] != null) {
        grid[writeRow][col] = grid[row][col];
        if (writeRow != row) {
          grid[row][col] = null;
        }
        writeRow--;
      }
    }
    for (int row = writeRow; row >= topRow; row--) {
      grid[row][col] = randomKind();
    }
  }

  private void ensureBoardIsPlayable() {
    if (hasAnyValidMove()) {
      return;
    }
    System.out.println("No moves left! The lawn reshuffles itself.");
    reshuffleBoard();
  }

  private boolean hasAnyValidMove() {
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        if (createsMatchWhenSwapped(row, col, row, col + 1)
                || createsMatchWhenSwapped(row, col, row + 1, col)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean createsMatchWhenSwapped(int rowA, int colA, int rowB, int colB) {
    if (!inBounds(rowA, colA) || !inBounds(rowB, colB)) {
      return false;
    }
    if (grid[rowA][colA] == null || grid[rowB][colB] == null) {
      return false;
    }
    swapCells(rowA, colA, rowB, colB);
    boolean creates = !findMatchGroups().isEmpty();
    swapCells(rowA, colA, rowB, colB);
    return creates;
  }

  private void swapCells(int rowA, int colA, int rowB, int colB) {
    PlantKind temp = grid[rowA][colA];
    grid[rowA][colA] = grid[rowB][colB];
    grid[rowB][colB] = temp;
  }

  private boolean isAdjacent(int rowA, int colA, int rowB, int colB) {
    return Math.abs(rowA - rowB) + Math.abs(colA - colB) == 1;
  }

  private boolean inBounds(int row, int col) {
    return row >= 0 && row < ROWS && col >= 0 && col < COLS;
  }

  public void tick() {
    if (won || lost) {
      return;
    }

    ticksSinceSpawn++;
    if (ticksSinceSpawn >= ticksBetweenSpawns) {
      ticksSinceSpawn = 0;
      zombies.add(new LaneZombie(random.nextInt(ROWS), COLS - 1, 100 + level * 40));
    }

    advanceZombies();
    zombies.removeIf(LaneZombie::isDead);
    checkEndConditions();
  }

  private void advanceZombies() {
    ticksSinceZombieAdvance++;
    if (ticksSinceZombieAdvance < ZOMBIE_MOVE_INTERVAL_TICKS) {
      return;
    }
    ticksSinceZombieAdvance = 0;

    for (LaneZombie zombie : zombies) {
      int col = (int) Math.round(zombie.column);
      if (inBounds(zombie.row, col) && grid[zombie.row][col] != null) {
        zombie.chewTicks += ZOMBIE_MOVE_INTERVAL_TICKS;
        if (zombie.chewTicks >= CHEW_TICKS) {
          zombie.chewTicks = 0;
          System.out.printf("A zombie ate the %s at (%d, %d); that tile is now a crater.%n",
                  grid[zombie.row][col].label, col + 1, zombie.row + 1);
          grid[zombie.row][col] = null;
          crater[zombie.row][col] = true;
          applyGravityAndRefill();
          ensureBoardIsPlayable();
        }
        continue;
      }
      zombie.column -= 1;
      if (zombie.column < 0) {
        lost = true;
        System.out.println("The zombie ate your brain; LOSER!!!");
        return;
      }
    }
  }

  private void checkEndConditions() {
    if (won || lost) {
      return;
    }
    if (matchesMade >= matchTarget) {
      won = true;
      zombies.clear();
      System.out.println("You matched your way to victory; every zombie on the lawn vanished!");
    }
  }

  public boolean isWon() {
    return won;
  }


  public boolean isFinished() {
    return won || lost;
  }

  public int getSun() {
    return sun;
  }

  public java.util.Collection<Upgrade> getUpgrades() {return upgrades.values();}
  public PlantKind getPlantAt(int row, int col) {
    return grid[row][col];
  }
  public boolean isCraterAt(int row, int col) {
    return crater[row][col];
  }
  public int getZombieHealthAt(int row, int col) {
    for (LaneZombie zombie : zombies) {
      if (zombie.row == row && (int) Math.round(zombie.column) == col && !zombie.isDead()) {
        return zombie.health;
      }
    }
    return -1;
  }
  public int getMatchesMade() {
    return matchesMade;
  }

  public int getMatchTarget() {
    return matchTarget;
  }

}
