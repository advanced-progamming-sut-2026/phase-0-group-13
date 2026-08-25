package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import data.GameDataManager;
import data.repository.PlantRepository;
import model.game.plant.PlantParts.PlantTemplate;


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
  public static final int TICKS_PER_SECOND = 10;
  /** What the defending player starts with, the same purse the attacker gets. */
  public static final int PLANT_STARTING_SUN = 150;
  /** The cutouts shoot once every 1.5s for a flat amount, which is what a Peashooter is. */
  public static final String CUTOUT_PLANT = "Peashooter";

  /**
   * What the defending player may place.
   *
   * <p>Four plants, because these are the four whose behaviour this engine can already express: a
   * producer, two shooters and a blocker. Anything carrying a mechanic of its own -- a slow, a
   * splash, a one-shot detonation -- would need the engine to grow one first, and inventing it in
   * a view or a network layer is exactly what this list exists to avoid.
   */
  public static final List<String> PLANT_ROSTER =
      List.of("Sunflower", "Peashooter", "Repeater", "Wall-nut");

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

  /**
   * A placeable plant, priced and statted from plants.json.
   *
   * <p>Nothing here is a number this class chose: cost, health, recharge and the shot's damage and
   * cadence are all read out of the same catalogue the adventure game is built from, so the two
   * sides of a match are costed against one another rather than against two different tables.
   */
  public static final class PlantSpec {
    public final String name;
    public final int cost;
    public final int health;
    public final int damagePerShot;
    public final int intervalTicks;
    public final int rechargeTicks;
    public final int sunPerCycle;

    private PlantSpec(String name, int cost, int health, int damagePerShot, int intervalTicks,
        int rechargeTicks, int sunPerCycle) {
      this.name = name;
      this.cost = cost;
      this.health = health;
      this.damagePerShot = damagePerShot;
      this.intervalTicks = intervalTicks;
      this.rechargeTicks = rechargeTicks;
      this.sunPerCycle = sunPerCycle;
    }

    public boolean producesSun() {
      return sunPerCycle > 0;
    }
  }

  /** "20" is one pea; the Repeater's "20x2" is two. */
  private static final Pattern DAMAGE = Pattern.compile("(\\d+)(?:\\s*[x*]\\s*(\\d+))?");
  private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");

  private static Map<String, PlantSpec> plantCatalog;

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

  /**
   * Built once, from the repository. If the game data has not been loaded yet -- which is the
   * server's case, since it never opens a lawn of its own -- loading it is what is missing, not a
   * reason to make the numbers up, so it is loaded here.
   */
  private static synchronized Map<String, PlantSpec> plantCatalog() {
    if (plantCatalog != null) {
      return plantCatalog;
    }
    if (GameDataManager.plantRepository == null) {
      new GameDataManager();
    }
    PlantRepository repository = GameDataManager.plantRepository;
    Map<String, PlantSpec> map = new LinkedHashMap<>();
    for (String name : PLANT_ROSTER) {
      PlantTemplate template = repository == null ? null : repository.find(name);
      if (template != null) {
        map.put(key(template.name), specOf(template));
      }
    }
    plantCatalog = map;
    return map;
  }

  private static PlantSpec specOf(PlantTemplate template) {
    return new PlantSpec(template.name, template.cost, template.baseHp, damageOf(template.damage),
        intervalTicksOf(template), Math.max(1, template.recharge) * TICKS_PER_SECOND,
        sunPerCycleOf(template));
  }

  private static int damageOf(String damage) {
    Matcher matcher = DAMAGE.matcher(damage == null ? "" : damage.trim());
    if (!matcher.matches()) {
      return 0;
    }
    int each = Integer.parseInt(matcher.group(1));
    return matcher.group(2) == null ? each : each * Integer.parseInt(matcher.group(2));
  }

  private static int intervalTicksOf(PlantTemplate template) {
    try {
      double seconds = Double.parseDouble(template.actionInterval.trim());
      return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    } catch (RuntimeException e) {
      return PLANT_FIRE_INTERVAL;
    }
  }

  /** "Produces 50 sun every 24 seconds." - the amount is the ability's own first number. */
  private static int sunPerCycleOf(PlantTemplate template) {
    if (!"Sun Producer".equalsIgnoreCase(template.category)) {
      return 0;
    }
    Matcher matcher = FIRST_NUMBER.matcher(template.baseAbility == null ? "" : template.baseAbility);
    return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
  }

  private static String key(String name) {
    return name == null ? "" : name.toLowerCase().trim();
  }

  private static List<ZombieSpec> purchasableSpecs() {
    List<ZombieSpec> purchasable = new ArrayList<>();
    for (ZombieSpec spec : CATALOG.values()) {
      if (!spec.producesSun) {purchasable.add(spec);}}
    return purchasable;}
  /** One of the player's zombies on the lawn. */
  public static final class DeployedZombie {
    private final int id;
    private final ZombieSpec spec;
    private final int row;
    private double column;
    private int health;
    private int sunTimer;
    private boolean eating;

    private DeployedZombie(int id, ZombieSpec spec, int row, double column) {
      this.id = id;
      this.spec = spec;
      this.row = row;
      this.column = column;
      this.health = spec.health;
      this.sunTimer = 0;
    }

    /**
     * Unique for the life of the engine.
     *
     * <p>A client is sent a fresh copy of the board every tick, so without this it cannot tell
     * that the zombie in the picture is the same one it drew a moment ago -- and an animation that
     * cannot be recognised between frames restarts on every one of them.
     */
    public int getId() {
      return id;
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

  /** One of the plants defending the brains: a seeded cutout, or one the defender placed. */
  public static final class DefensePlant {
    private final int id;
    private final String name;
    private final int row;
    private final int col;
    private int health;
    private final int maxHealth;
    private final int damagePerTick;
    private final int intervalTicks;
    private final int sunPerCycle;
    private final double range;
    private int timer;

    private DefensePlant(int id, String name, int row, int col, int health, int damagePerTick,
        int intervalTicks, int sunPerCycle, double range) {
      this.id = id;
      this.name = name;
      this.row = row;
      this.col = col;
      this.health = health;
      this.maxHealth = health;
      this.damagePerTick = damagePerTick;
      this.intervalTicks = intervalTicks;
      this.sunPerCycle = sunPerCycle;
      this.range = range;
    }

    private boolean isDead() {
      return health <= 0;
    }

    /** Unique for the life of the engine, same reason as {@link DeployedZombie#getId()}. */
    public int getId() {
      return id;
    }

    /** What it is, so a view can draw it as itself rather than as one stand-in for all of them. */
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

    public int getMaxHealth() {
      return maxHealth;
    }

    public boolean producesSun() {
      return sunPerCycle > 0;
    }
  }

  private final List<DeployedZombie> deployedZombies = new ArrayList<>();
  private final List<DefensePlant> defensePlants = new ArrayList<>();
  private final Map<String, Integer> rechargeLeft = new HashMap<>();
  private final Map<String, Integer> plantRechargeLeft = new HashMap<>();
  private final boolean[] brainAlive = new boolean[BRAINS];
  private final Random random;
  private final int level;
  private int zombieSun;
  private int plantSun;
  private int nextEntityId;
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
    this.plantSun = PLANT_STARTING_SUN;
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
        defensePlants.add(new DefensePlant(nextEntityId++, CUTOUT_PLANT, row, columns.get(i),
                120 + level * 30, 12 + level * 3, PLANT_FIRE_INTERVAL, 0, 5.0));
      }
    }
  }
  private void seedSunProducers() {
    ZombieSpec spec = CATALOG.get(SUN_PRODUCER);
    for (int row = 0; row < ROWS; row++) {
      deployedZombies.add(new DeployedZombie(nextEntityId++, spec, row, COLS - 1));
      System.out.printf("A %s takes its place in lane %d.%n", SUN_PRODUCER, row + 1);
    }
  }

  public List<ZombieSpec> availableZombieTypes() {
    return zombieTypesFor(level);
  }

  /** The same roster without a running engine, for a client that only knows the level. */
  public static List<ZombieSpec> zombieTypesFor(int level) {
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
    deployedZombies.add(new DeployedZombie(nextEntityId++, spec, row, col));
    return "Deployed " + typeName + " at (" + (col + 1) + ", " + (row + 1) + "). Zombie-sun left: "
        + zombieSun;
  }
  /**
   * The defending player's move, refused in the same words and the same order as placeZombie: the
   * cell, the type, what is already standing there, the recharge, then the price.
   *
   * @return null-free; a message starting with "error:" is a refusal, anything else is the receipt
   */
  public String placePlant(String typeName, int row, int col) {
    if (isFinished()) {
      return "error: the match is over";
    }
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
      return "error: coordinates out of bounds";
    }
    if (col >= RED_LINE_COLUMN) {
      return "error: plants can only be placed left of the red line (columns 1-"
              + RED_LINE_COLUMN + ")";
    }
    PlantSpec spec = plantCatalog().get(key(typeName));
    if (spec == null) {
      return "error: unknown plant type '" + typeName + "'";
    }
    for (DefensePlant planted : defensePlants) {
      if (planted.row == row && planted.col == col && !planted.isDead()) {
        return "error: there is already a " + planted.name + " at (" + (col + 1) + ", "
                + (row + 1) + ")";
      }
    }
    for (DeployedZombie zombie : deployedZombies) {
      if (zombie.row == row && !zombie.isDead() && Math.abs(zombie.column - col) < 0.5) {
        return "error: a zombie is standing on (" + (col + 1) + ", " + (row + 1) + ")";
      }
    }
    int waiting = plantRechargeTicksLeft(spec.name);
    if (waiting > 0) {
      return String.format("error: %s is still recharging (%.1fs left)", spec.name,
              waiting / (double) TICKS_PER_SECOND);
    }
    if (plantSun < spec.cost) {
      return "error: not enough sun (need " + spec.cost + ", have " + plantSun + ")";
    }
    plantSun -= spec.cost;
    plantRechargeLeft.put(key(spec.name), spec.rechargeTicks);
    defensePlants.add(new DefensePlant(nextEntityId++, spec.name, row, col, spec.health,
            spec.damagePerShot, spec.intervalTicks, spec.sunPerCycle, 5.0));
    return "Planted " + spec.name + " at (" + (col + 1) + ", " + (row + 1) + "). Sun left: "
            + plantSun;
  }

  public void tick() {
    if (won || lost) {return;}
    tickCount++;
    rechargeLeft.replaceAll((type, left) -> Math.max(0, left - 1));
    plantRechargeLeft.replaceAll((type, left) -> Math.max(0, left - 1));
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
      if (plant.isDead()) {
        continue;
      }
      plant.timer++;
      if (plant.timer < plant.intervalTicks) {
        continue;
      }
      plant.timer = 0;
      if (plant.producesSun()) {
        plantSun += plant.sunPerCycle;
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

  /** The same, for the defending player's side of the board. */
  public int plantRechargeTicksLeft(String typeName) {
    Integer left = plantRechargeLeft.get(key(typeName));
    return left == null ? 0 : left;
  }

  public static List<PlantSpec> availablePlantTypes() {
    return List.copyOf(plantCatalog().values());
  }

  public int getPlantSun() {
    return plantSun;
  }

  public int getTickCount() {
    return tickCount;
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