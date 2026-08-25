package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public final class IZombieEngine {

  public static final int ROWS = 5;
  public static final int COLS = 9;
  public static final int BRAINS = 5;
  public static final int STARTING_SUN = 150;
  public static final int RED_LINE_COLUMN = 4;
  public static final String SUN_PRODUCER = "sun-imp";
  /**
   * Ticks a type has to wait before it can be deployed again, one per sun of its cost, so a
   * basic is back in five seconds and a gargantuar in thirty. Zombies had no recharge at all
   * until now, which left the picker with nothing truthful to show for the cooldown the doc
   * asks it to display.
   */
  public static final int RECHARGE_TICKS_PER_SUN = 1;
  // Cutout plants fire on the same cadence as every other plant in the game: one shot per
  // 1.5s (15 ticks at 10 ticks/second), matching the Peashooter's Action Interval in plants.json.
  public static final int PLANT_FIRE_INTERVAL = 15;

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

  public static final int BUCKETHEAD_HEALTH = 1290;

  private static final Map<String, ZombieSpec> CATALOG = buildCatalog();
  private static final int[][] STAGE_PICKS = {{0, 1, 2, 3, 4}, {1, 3, 5, 6, 7}, {3, 5, 7, 8, 9}};

  private static Map<String, ZombieSpec> buildCatalog() {
    Map<String, ZombieSpec> map = new LinkedHashMap<>();
    map.put("basic", new ZombieSpec("basic", 50, 90, 8, 0.05, false, 1));
    map.put("conehead", new ZombieSpec("conehead", 75, 160, 8, 0.05, false, 1));
    map.put("pole-vaulter", new ZombieSpec("pole-vaulter", 100, 110, 10, 0.09, false, 1));
    map.put("buckethead", new ZombieSpec("buckethead", 150, 260, 8, 0.05, false, 1));
    map.put("newspaper", new ZombieSpec("newspaper", 100, 190, 10, 0.07, false, 1));

    map.put("football", new ZombieSpec("football", 175, 400, 14, 0.08, false, 2));
    map.put("digger", new ZombieSpec("digger", 125, 130, 10, 0.07, false, 2));
    map.put("screen-door", new ZombieSpec("screen-door", 150, 220, 8, 0.05, false, 2));
    map.put("ladder", new ZombieSpec("ladder", 150, 150, 9, 0.06, false, 2));
    map.put("gargantuar", new ZombieSpec("gargantuar", 300, 1400, 40, 0.03, false, 2));

    map.put(SUN_PRODUCER, new ZombieSpec(SUN_PRODUCER, 0, BUCKETHEAD_HEALTH, 0, 0.0, true, 1));
    return map;
  }

  private static List<ZombieSpec> purchasableSpecs() {
    List<ZombieSpec> purchasable = new ArrayList<>();
    for (ZombieSpec spec : CATALOG.values()) {
      if (!spec.producesSun) {purchasable.add(spec);}}
    return purchasable;}
  /** One of the player's zombies on the lawn. */
  public static final class DeployedZombie {
    private final ZombieSpec spec;
    private final int row;
    private double column;
    private int health;
    private int sunTimer;
    private boolean eating;

    private DeployedZombie(ZombieSpec spec, int row, double column) {
      this.spec = spec;
      this.row = row;
      this.column = column;
      this.health = spec.health;
      this.sunTimer = 0;
    }

    public boolean isDead() {
      return health <= 0;
    }

    public String getName() {
      return spec.name;
    }

    public int getRow() {
      return row;
    }

    /** Unrounded, so a view can draw the walk rather than a jump per cell. */
    public double getColumn() {
      return column;
    }

    public int getHealth() {
      return health;
    }

    public int getMaxHealth() {
      return spec.health;
    }

    public boolean producesSun() {
      return spec.producesSun;
    }

    /** True while it is chewing a cutout instead of walking. */
    public boolean isEating() {
      return eating;
    }
  }

  /** One of the cardboard plant cutouts defending the brains. */
  public static final class DefensePlant {
    private final int row;
    private final int col;
    private int health;
    private final int maxHealth;
    private final int damagePerTick;
    private final double range;

    private DefensePlant(int row, int col, int health, int damagePerTick, double range) {
      this.row = row;
      this.col = col;
      this.health = health;
      this.maxHealth = health;
      this.damagePerTick = damagePerTick;
      this.range = range;
    }

    private boolean isDead() {
      return health <= 0;
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

    public int getMaxHealth() {
      return maxHealth;
    }
  }

  private final List<DeployedZombie> deployedZombies = new ArrayList<>();
  private final List<DefensePlant> defensePlants = new ArrayList<>();
  private final Map<String, Integer> rechargeLeft = new HashMap<>();
  private final boolean[] brainAlive = new boolean[BRAINS];
  private final Random random;
  private final int level;
  private int zombieSun;
  private int tickCount;
  private boolean won;
  private boolean lost;

  public IZombieEngine(int level) {
    this(level, new Random());
  }
  public IZombieEngine(int level, Random random) {
    this.level = level;
    this.random = random;
    this.zombieSun = STARTING_SUN;
    for (int i = 0; i < BRAINS; i++) {
      brainAlive[i] = true;
    }
    seedDefensivePlants();
    seedSunProducers();
  }

  private void seedDefensivePlants() {

    int plantsPerRow = 1 + level;
    for (int row = 0; row < ROWS; row++) {
      List<Integer> columns = new ArrayList<>();
      for (int col = 0; col < RED_LINE_COLUMN; col++) {
        columns.add(col);
      }
      java.util.Collections.shuffle(columns, random);
      for (int i = 0; i < plantsPerRow && i < columns.size(); i++) {
        defensePlants.add(
                new DefensePlant(row, columns.get(i), 120 + level * 30, 12 + level * 3, 5.0));
      }
    }
  }
  private void seedSunProducers() {
    ZombieSpec spec = CATALOG.get(SUN_PRODUCER);
    for (int row = 0; row < ROWS; row++) {
      deployedZombies.add(new DeployedZombie(spec, row, COLS - 1));
      System.out.printf("A %s takes its place in lane %d.%n", SUN_PRODUCER, row + 1);
    }
  }

  public List<ZombieSpec> availableZombieTypes() {
    List<ZombieSpec> all = purchasableSpecs();
    List<ZombieSpec> available = new ArrayList<>();
    for (int index : STAGE_PICKS[Math.min(Math.max(level, 1), STAGE_PICKS.length) - 1]) {
      available.add(all.get(index));
    }
    return available;
  }

  public String placeZombie(String typeName, int row, int col) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    if (col <= RED_LINE_COLUMN) {
      return "error: zombies can only be placed to the right of the red line (columns "
              + (RED_LINE_COLUMN + 2) + "-" + COLS + ")";
    }
    ZombieSpec spec = CATALOG.get(typeName.toLowerCase().trim());
    if (spec == null) {
      return "error: unknown zombie type '" + typeName + "'";
    }
    if (spec.producesSun) {
      return "error: " + typeName + " cannot be bought; one already guards every lane";
    }
    boolean unlocked = availableZombieTypes().contains(spec);
    if (!unlocked) {
      return "error: " + typeName + " isn't available at level " + level;
    }
    int waiting = rechargeTicksLeft(spec.name);
    if (waiting > 0) {
      return String.format("error: %s is still recharging (%.1fs left)", typeName, waiting / 10.0);
    }
    if (zombieSun < spec.cost) {
      return "error: not enough zombie-sun (need " + spec.cost + ", have " + zombieSun + ")";
    }
    zombieSun -= spec.cost;
    rechargeLeft.put(spec.name, rechargeTicks(spec));
    deployedZombies.add(new DeployedZombie(spec, row, col));
    return "Deployed " + typeName + " at (" + (col + 1) + ", " + (row + 1) + "). Zombie-sun left: "
        + zombieSun;
  }
  public void tick() {
    if (won || lost) {return;}
    tickCount++;
    rechargeLeft.replaceAll((type, left) -> Math.max(0, left - 1));
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.isDead() || !zombie.spec.producesSun) {
        continue;
      }
      zombie.sunTimer++;
      if (zombie.sunTimer >= Math.max(8, 48 - tickCount / 100)) {
        zombie.sunTimer = 0;
        zombieSun += 25;
        System.out.printf("zombie %s produced a sun at (%d, %d); you now have %d sun.%n",
                zombie.spec.name, (int) Math.round(zombie.column) + 1, zombie.row + 1, zombieSun);
      }
    }
    for (DefensePlant plant : defensePlants) {
      if (plant.isDead() || tickCount % PLANT_FIRE_INTERVAL != 0) {
        continue;
      }
      DeployedZombie target = nearestZombieAhead(plant);
      if (target != null) {
        target.health -= plant.damagePerTick;
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
      zombie.eating = blocker != null;
      if (blocker != null) {blocker.health -= zombie.spec.damagePerTick;
      } else {zombie.column -= zombie.spec.speed;
        if (zombie.column <= 0) {
          eatBrain(zombie.row);
          zombie.health = 0;
        }
      }
    }
    reportCasualties();
    defensePlants.removeIf(DefensePlant::isDead);
    deployedZombies.removeIf(DeployedZombie::isDead);
    checkEndConditions();
  }

  private DeployedZombie nearestZombieAhead(DefensePlant plant) {
    DeployedZombie nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.row != plant.row || zombie.isDead()) {
        continue;
      }
      double distance = zombie.column - plant.col;
      if (distance >= 0 && distance <= plant.range && distance < nearestDistance) {
        nearest = zombie;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  private void reportCasualties() {
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.isDead()) {
        System.out.printf("Zombie of type %s is dead at (%d, %d).%n",
                zombie.spec.name, (int) Math.round(zombie.column) + 1, zombie.row + 1);
      }
    }
    for (DefensePlant plant : defensePlants) {
      if (plant.isDead()) {
        System.out.printf("Plant cutout at (%d, %d) is destroyed.%n", plant.col + 1, plant.row + 1);
      }
    }
  }

  private void eatBrain(int row) {
    int brainIndex = Math.min(row, BRAINS - 1);
    if (brainAlive[brainIndex]) {
      brainAlive[brainIndex] = false;
      System.out.printf("The brain in lane %d has been eaten; %d left.%n",
              row + 1, getBrainsRemaining());
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
      System.out.println("All five brains are eaten. You win!");
      return;
    }

    boolean canStillAct = zombieSun >= cheapestAvailableCost() || !deployedZombies.isEmpty();
    if (!canStillAct) {
      lost = true;
      System.out.println("No sun, no zombies left and nothing you can place; you lose.");
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

  /** How long this type still has to wait before it can be deployed again, in ticks. */
  public int rechargeTicksLeft(String typeName) {
    Integer left = rechargeLeft.get(typeName == null ? "" : typeName.toLowerCase().trim());
    return left == null ? 0 : left;
  }

  public static int rechargeTicks(ZombieSpec spec) {
    return spec.cost * RECHARGE_TICKS_PER_SUN;
  }

  public List<DeployedZombie> getDeployedZombies() {
    return deployedZombies;
  }

  public List<DefensePlant> getDefensePlants() {
    return defensePlants;
  }

  public int getZombieSun() {
    return zombieSun;
  }

  public boolean isBrainAlive(int row) {
    return brainAlive[Math.min(row, BRAINS - 1)];
  }
  public int getPlantHealthAt(int row, int col) {
    for (DefensePlant plant : defensePlants) {
      if (plant.row == row && plant.col == col && !plant.isDead()) {
        return plant.health;}}return -1;}
  public int getZombieHealthAt(int row, int col) {
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.row == row && (int) Math.round(zombie.column) == col && !zombie.isDead()) {
        return zombie.health;}}return -1;}

  public int getBrainsRemaining() {
    int count = 0;
    for (boolean alive : brainAlive) {
      if (alive) {
        count++;
      }
    }
    return count;
  }

}