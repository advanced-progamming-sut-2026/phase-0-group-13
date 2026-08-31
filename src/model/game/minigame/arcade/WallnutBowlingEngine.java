package model.game.minigame.arcade;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.game.minigame.ConveyorRule;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.ZombieParts.ZombieTemplate;

public final class WallnutBowlingEngine {

  public static final int LANES = 5;
  public static final int LANE_LENGTH = 9;
  public static final int RED_LINE_COLUMN = 2;
  private static final int BELT_INTERVAL_TICKS = 120;
  private static final int FALLBACK_NORMAL_ZOMBIE_HEALTH = 190;
  private static final int FALLBACK_CHERRY_BOMB_DAMAGE = 1800;

  public enum NutType {
    NORMAL("bowling wall-nut", 'O'),
    EXPLODE_O_NUT("explode-o-nut", 'E'),
    GIANT("giant wall-nut", 'G');
    public final String label;
    public final char glyph;
    NutType(String label, char glyph) {
      this.label = label;
      this.glyph = glyph;
    }
    static NutType byLabel(String name) {
      for (NutType type : values()) {
        if (type.label.equalsIgnoreCase(name == null ? "" : name.trim())) {
          return type;}}return NORMAL;}
  }

  public static final class LaneZombie {
    private final String name;
    private final int lane;
    private int column;
    private int health;
    private final int maxHealth;

    private LaneZombie(String name, int lane, int column, int health) {
      this.name = name;
      this.lane = lane;
      this.column = column;
      this.health = health;
      this.maxHealth = health;
    }

    public String getName() {return name;}
    public int getLane() {
      return lane;
    }

    public int getColumn() {
      return column;
    }

    public int getHealth() {
      return health;
    }

    public int getMaxHealth() {
      return maxHealth;
    }

    private boolean isDead() {
      return health <= 0;
    }
  }

  private static final class RollingNut {
    private final NutType type;
    private int lane;
    private int column;
    private int dLane;
    private int dColumn;
    private int bounceStage;
    private boolean spent;

    private RollingNut(NutType type, int lane, int column) {
      this.type = type;
      this.lane = lane;
      this.column = column;
      this.dLane = 0;
      this.dColumn = 1;
    }
  }

  private final List<LaneZombie> zombies = new ArrayList<>();
  private final List<RollingNut> activeNuts = new ArrayList<>();
  private final ConveyorRule conveyor;
  private final Random random;
  private final int level;
  private final int nutDamage;
  private final int explosionDamage;
  private int zombiesRemainingToSpawn;
  private final int ticksBetweenSpawns;
  private int ticksSinceLastSpawn;
  private int ticksSinceZombieAdvance;
  private static final int ZOMBIE_MOVE_INTERVAL_TICKS = 30;
  private int score;
  private boolean won;
  private boolean lost;
  private static final int LOSE_THRESHOLD_COLUMN = 0;

  public WallnutBowlingEngine(int level) {
    this(level, new Random());
  }

  public WallnutBowlingEngine(int level, Random random) {
    this.level = level;
    this.random = random;
    this.nutDamage = normalZombieHealth();
    this.explosionDamage = cherryBombDamage();
    this.zombiesRemainingToSpawn = 8 + level * 4;
    this.ticksBetweenSpawns = Math.max(15, 45 - level * 10);
    this.conveyor = new ConveyorRule(
            List.of(NutType.NORMAL.label, NutType.EXPLODE_O_NUT.label, NutType.GIANT.label),
            BELT_INTERVAL_TICKS);
    this.conveyor.deliverNow();
    spawnWave(2 + level);
  }

  private static int normalZombieHealth() {
    if (GameDataManager.zombieRepository != null) {
      ZombieTemplate template = GameDataManager.zombieRepository.find("ZombieMummyDefault");
      if (template != null && template.getBaseHp() > 0) {
        return template.getBaseHp();
      }
    }
    return FALLBACK_NORMAL_ZOMBIE_HEALTH;
  }
  private static int cherryBombDamage() {
    if (GameDataManager.plantRepository != null) {
      PlantTemplate template = GameDataManager.plantRepository.find("Cherry Bomb");
      if (template != null && template.damage != null) {
        try {
          return Integer.parseInt(template.damage.trim());
        } catch (NumberFormatException e) {
          return FALLBACK_CHERRY_BOMB_DAMAGE;
        }
      }
    }
    return FALLBACK_CHERRY_BOMB_DAMAGE;
  }
  private void spawnWave(int count) {
    for (int i = 0; i < count && zombiesRemainingToSpawn > 0; i++) {
      int lane = random.nextInt(LANES);
      int health = normalZombieHealth() * level;
      zombies.add(new LaneZombie("Zombie", lane, LANE_LENGTH - 1, health));
      zombiesRemainingToSpawn--;
    }
  }

  public String plantNut(int lane, int column) {
    if (lane < 0 || lane >= LANES) {
      return "error: lane out of bounds (1-" + LANES + ")";
    }
    if (column < 0 || column > RED_LINE_COLUMN) {
      return "error: you can only plant before the red line (columns 1-"
              + (RED_LINE_COLUMN + 1) + ")";
    }
    String delivered = conveyor.consumeReadyPlant();
    if (delivered == null) {
      return "error: the conveyor belt has not delivered a nut yet";
    }
    NutType type = NutType.byLabel(delivered);
    activeNuts.add(new RollingNut(type, lane, column));
    return String.format("%s planted at (%d, %d); it starts rolling down the lane.",
            type.label, column + 1, lane + 1);
  }

  public String getReadyNutLabel() {
    return conveyor.isPlantAllowed(NutType.NORMAL.label) ? NutType.NORMAL.label
            : conveyor.isPlantAllowed(NutType.EXPLODE_O_NUT.label) ? NutType.EXPLODE_O_NUT.label
            : conveyor.isPlantAllowed(NutType.GIANT.label) ? NutType.GIANT.label : "nothing yet";
  }

  public void tick() {
    if (won || lost) {
      return;
    }

    conveyor.apply(null);
    ticksSinceLastSpawn++;
    if (ticksSinceLastSpawn >= ticksBetweenSpawns && zombiesRemainingToSpawn > 0) {
      spawnWave(1 + level / 2);
      ticksSinceLastSpawn = 0;
    }

    advanceZombies();

    for (RollingNut nut : activeNuts) {
      bouncePhysics(nut);
      checkCollision(nut);
    }
    activeNuts.removeIf(n -> n.spent);
    reportCasualties();
    zombies.removeIf(LaneZombie::isDead);

    if (checkLoseCondition()) {
      lost = true;
      System.out.println("The zombie ate your brain; LOSER!!!");
      return;
    }

    if (zombiesRemainingToSpawn <= 0 && zombies.isEmpty()) {
      won = true;
      System.out.println("Every zombie is bowled over. You win!");
    }
  }
  private void reportCasualties() {
    for (LaneZombie zombie : zombies) {
      if (zombie.isDead()) {
        System.out.printf("Zombie of type %s is dead at (%d, %d).%n",
                zombie.name, zombie.column + 1, zombie.lane + 1);}}
  }

  private void advanceZombies() {
    ticksSinceZombieAdvance++;
    if (ticksSinceZombieAdvance < ZOMBIE_MOVE_INTERVAL_TICKS) {
      return;
    }
    ticksSinceZombieAdvance = 0;
    for (LaneZombie zombie : zombies) {
      if (!zombie.isDead() && zombie.column > LOSE_THRESHOLD_COLUMN) {
        zombie.column--;
      }
    }
  }

  private boolean checkLoseCondition() {
    for (LaneZombie zombie : zombies) {
      if (!zombie.isDead() && zombie.column <= LOSE_THRESHOLD_COLUMN) {
        return true;
      }
    }
    return false;
  }


  private void bouncePhysics(RollingNut nut) {
    if (nut.spent) {
      return;
    }

    int nextLane = nut.lane + nut.dLane;
    if (nextLane < 0 || nextLane >= LANES) {
      nut.dLane = -nut.dLane;
      nextLane = nut.lane + nut.dLane;
      System.out.printf("The %s bounced off the lawn edge at lane %d.%n",
              nut.type.label, nut.lane + 1);
    }
    nut.lane = Math.max(0, Math.min(LANES - 1, nextLane));
    nut.column += nut.dColumn;

    if (nut.column < 0 || nut.column >= LANE_LENGTH) {
      nut.spent = true;
    }
  }

  private void checkCollision(RollingNut nut) {
    if (nut.spent) {
      return;
    }
    LaneZombie hit = findZombieAt(nut.lane, nut.column);
    if (hit == null) {
      return;
    }

    switch (nut.type) {
      case NORMAL:
        handleNormalNutHit(nut, hit);
        break;
      case EXPLODE_O_NUT:
        handleExplodeNutHit(nut, hit);
        break;
      case GIANT:
        handleGiantNutHit(hit);
        break;
      default:
        break;
    }
  }

  private void handleNormalNutHit(RollingNut nut, LaneZombie hit) {
    hit.health -= nutDamage;
    score += 10;
    rotateTrajectory(nut);
  }

  private void rotateTrajectory(RollingNut nut) {
    if (nut.bounceStage == 0) {
      nut.bounceStage = 1;
      nut.dLane = nut.lane >= LANES - 1 ? -1 : 1;
      System.out.printf("The %s bounced 45 degrees at (%d, %d).%n",
              nut.type.label, nut.column + 1, nut.lane + 1);
      return;
    }
    nut.bounceStage++;
    nut.dLane = -nut.dLane;
    System.out.printf("The %s bounced 90 degrees at (%d, %d).%n",
            nut.type.label, nut.column + 1, nut.lane + 1);
  }

  private void handleExplodeNutHit(RollingNut nut, LaneZombie hit) {
    for (LaneZombie zombie : zombies) {
      boolean inBlastLane = Math.abs(zombie.lane - nut.lane) <= 1;
      boolean inBlastColumn = Math.abs(zombie.column - nut.column) <= 1;
      if (inBlastLane && inBlastColumn) {
        zombie.health -= explosionDamage;
      }
    }
    score += 60;
    nut.spent = true;
    System.out.printf("The %s exploded at (%d, %d) over a 3x3 area for %d damage.%n",
            nut.type.label, nut.column + 1, nut.lane + 1, explosionDamage);
  }

  private void handleGiantNutHit(LaneZombie hit) {
    hit.health = 0;
    score += 25;
    System.out.printf("The %s crushed a zombie at (%d, %d) and rolls straight on.%n",
            NutType.GIANT.label, hit.column + 1, hit.lane + 1);
  }

  private LaneZombie findZombieAt(int lane, int column) {
    for (LaneZombie zombie : zombies) {
      if (!zombie.isDead() && zombie.lane == lane && zombie.column == column) {
        return zombie;
      }
    }
    return null;
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

  public int getScore() {
    return score;
  }

  public List<LaneZombie> getZombies() {
    return zombies;
  }

  public int getZombiesRemainingToSpawn() {
    return zombiesRemainingToSpawn;
  }

  public int getLevel() {
    return level;
  }

  public int getZombieHealthAt(int lane, int column) {
    LaneZombie zombie = findZombieAt(lane, column);
    return zombie == null ? -1 : zombie.health;
  }

  public NutType getNutTypeAt(int lane, int column) {
    for (RollingNut nut : activeNuts) {
      if (!nut.spent && nut.lane == lane && nut.column == column) {
        return nut.type;
      }
    }
    return null;
  }
}
