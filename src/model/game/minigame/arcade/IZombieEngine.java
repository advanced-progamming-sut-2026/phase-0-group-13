package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class IZombieEngine {

  public static final int ROWS = 5;
  public static final int COLS = 9;
  public static final int BRAINS = 5;
  public static final String SUN_PRODUCER = "sun-imp";

  public static final class ZombieSpec {
    public final String name;
    public final int cost;
    public final int health;
    public final int damagePerTick;
    public final double speed; // columns per tick
    public final boolean producesSun;
    public final int unlockLevel;

    private ZombieSpec(String name, int cost, int health, int damagePerTick, double speed,
        boolean producesSun, int unlockLevel) {
      this.name = name;
      this.cost = cost;
      this.health = health;
      this.damagePerTick = damagePerTick;
      this.speed = speed;
      this.producesSun = producesSun;
      this.unlockLevel = unlockLevel;
    }
  }

  private static final Map<String, ZombieSpec> CATALOG = buildCatalog();

  private static Map<String, ZombieSpec> buildCatalog() {
    Map<String, ZombieSpec> map = new LinkedHashMap<>();
    map.put("basic", new ZombieSpec("basic", 50, 90, 8, 0.05, false, 1));
    map.put("conehead", new ZombieSpec("conehead", 75, 160, 8, 0.05, false, 1));
    map.put("pole-vaulter", new ZombieSpec("pole-vaulter", 100, 110, 10, 0.09, false, 1));
    map.put(SUN_PRODUCER, new ZombieSpec(SUN_PRODUCER, 125, 70, 0, 0.0, true, 1));
    map.put("buckethead", new ZombieSpec("buckethead", 150, 260, 8, 0.05, false, 1));

    map.put("football", new ZombieSpec("football", 175, 400, 14, 0.08, false, 2));
    map.put("digger", new ZombieSpec("digger", 125, 130, 10, 0.07, false, 2));
    map.put("screen-door", new ZombieSpec("screen-door", 150, 220, 8, 0.05, false, 2));
    map.put("ladder", new ZombieSpec("ladder", 150, 150, 9, 0.06, false, 2));
    map.put("gargantuar", new ZombieSpec("gargantuar", 300, 1400, 40, 0.03, false, 2));
    return map;
  }

  private static final class DeployedZombie {
    private final ZombieSpec spec;
    private final int row;
    private double column;
    private int health;
    private int sunTimer;

    private DeployedZombie(ZombieSpec spec, int row, double column) {
      this.spec = spec;
      this.row = row;
      this.column = column;
      this.health = spec.health;
      this.sunTimer = 0;
    }

    private boolean isDead() {
      return health <= 0;
    }
  }

  private static final class DefensePlant {
    private final int row;
    private final int col;
    private int health;
    private final int damagePerTick;
    private final double range;

    private DefensePlant(int row, int col, int health, int damagePerTick, double range) {
      this.row = row;
      this.col = col;
      this.health = health;
      this.damagePerTick = damagePerTick;
      this.range = range;
    }

    private boolean isDead() {
      return health <= 0;
    }
  }

  private final List<DeployedZombie> deployedZombies = new ArrayList<>();
  private final List<DefensePlant> defensePlants = new ArrayList<>();
  private final boolean[] brainAlive = new boolean[BRAINS];
  private final int level;
  private int zombieSun;
  private boolean won;
  private boolean lost;

  public IZombieEngine(int level) {
    this.level = level;
    this.zombieSun = 150 + level * 50;
    for (int i = 0; i < BRAINS; i++) {
      brainAlive[i] = true;
    }
    seedDefensivePlants();
  }

  private void seedDefensivePlants() {

    int plantsPerRow = 1 + level;
    for (int row = 0; row < ROWS; row++) {
      for (int i = 0; i < plantsPerRow; i++) {
        int col = COLS - 2 - i * 2;
        if (col < 1) {
          break;
        }
        defensePlants.add(new DefensePlant(row, col, 120 + level * 30, 12 + level * 3, 5.0));
      }
    }
  }

  public List<ZombieSpec> availableZombieTypes() {
    List<ZombieSpec> available = new ArrayList<>();
    for (ZombieSpec spec : CATALOG.values()) {
      if (level >= 3 || spec.unlockLevel <= level) {
        available.add(spec);
      }
    }
    return available;
  }

  public String placeZombie(String typeName, int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    ZombieSpec spec = CATALOG.get(typeName.toLowerCase().trim());
    if (spec == null) {
      return "error: unknown zombie type '" + typeName + "'";
    }
    boolean unlocked = level >= 3 || spec.unlockLevel <= level;
    if (!unlocked) {
      return "error: " + typeName + " isn't unlocked until level " + spec.unlockLevel;
    }
    if (zombieSun < spec.cost) {
      return "error: not enough zombie-sun (need " + spec.cost + ", have " + zombieSun + ")";
    }
    zombieSun -= spec.cost;
    deployedZombies.add(new DeployedZombie(spec, row, col));
    return "Deployed " + typeName + " at (" + (col + 1) + ", " + (row + 1) + "). Zombie-sun left: "
        + zombieSun;
  }
  public void tick() {
    if (won || lost) {return;}
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.isDead() || !zombie.spec.producesSun) {
        continue;
      }
      zombie.sunTimer++;
      if (zombie.sunTimer >= 24) {
        zombie.sunTimer = 0;
        zombieSun += 25;
      }
    }
    for (DefensePlant plant : defensePlants) {
      if (plant.isDead()) {
        continue;
      }
      for (DeployedZombie zombie : deployedZombies) {
        if (zombie.row != plant.row || zombie.isDead()) {continue;}
        double distance = plant.col - zombie.column;
        if (distance >= 0 && distance <= plant.range) {
          zombie.health -= plant.damagePerTick;
          break;
        }
      }
    }
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.isDead() || zombie.spec.producesSun) {
        continue;
      }
      DefensePlant blocker = null;
      for (DefensePlant plant : defensePlants) {
        if (!plant.isDead() && plant.row == zombie.row
            && Math.abs(plant.col - zombie.column) < 0.5) {
          blocker = plant;
          break;
        }
      }
      if (blocker != null) {blocker.health -= zombie.spec.damagePerTick;
      } else {zombie.column -= zombie.spec.speed;
        if (zombie.column <= 0) {
          eatBrain(zombie.row);
          zombie.health = 0;
        }
      }
    }
    defensePlants.removeIf(DefensePlant::isDead);
    deployedZombies.removeIf(DeployedZombie::isDead);
    checkEndConditions();
  }

  private void eatBrain(int row) {
    int brainIndex = Math.min(row, BRAINS - 1);
    if (brainAlive[brainIndex]) {
      brainAlive[brainIndex] = false;
    }
  }

  private void checkEndConditions() {
    boolean anyBrainAlive = false;
    for (boolean alive : brainAlive) {
      if (alive) {
        anyBrainAlive = true;
        break;
      }
    }
    if (!anyBrainAlive) {
      won = true;
      return;
    }

    boolean canStillAct = zombieSun >= cheapestAvailableCost() || !deployedZombies.isEmpty();
    if (!canStillAct) {
      lost = true;
    }
  }

  private int cheapestAvailableCost() {
    int cheapest = Integer.MAX_VALUE;
    for (ZombieSpec spec : availableZombieTypes()) {
      cheapest = Math.min(cheapest, spec.cost);
    }
    return cheapest;
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

  public int getZombieSun() {
    return zombieSun;
  }

  public int getBrainsRemaining() {
    int count = 0;
    for (boolean alive : brainAlive) {
      if (alive) {
        count++;
      }
    }
    return count;
  }

  public String renderMap() {
    StringBuilder sb = new StringBuilder();
    sb.append("--- I, Zombie: Level ").append(level).append(" | Zombie-Sun: ").append(zombieSun)
        .append(" | Brains left: ").append(getBrainsRemaining()).append("/").append(BRAINS)
        .append(" ---\n");
    for (int row = 0; row < ROWS; row++) {
      StringBuilder line = new StringBuilder();
      line.append(brainAlive[Math.min(row, BRAINS - 1)] ? "(B)" : "( )");
      for (int col = 0; col < COLS; col++) {
        char glyph = '.';
        for (DefensePlant p : defensePlants) {
          if (!p.isDead() && p.row == row && p.col == col) {
            glyph = 'P';
              break;
          }
        }
        for (DeployedZombie z : deployedZombies) {
          if (!z.isDead() && z.row == row && (int) Math.round(z.column) == col) {
            glyph = 'Z';
              break;
          }
        }
        line.append('[').append(glyph).append(']');
      }
      sb.append(line).append('\n');
    }
    sb.append("Available: ");
    for (ZombieSpec spec : availableZombieTypes()) {
      sb.append(spec.name).append('(').append(spec.cost).append(") ");
    }
    sb.append('\n');
    return sb.toString();
  }
}