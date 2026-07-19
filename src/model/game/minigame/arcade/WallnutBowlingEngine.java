package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WallnutBowlingEngine {

  public static final int LANES = 5;
  public static final int LANE_LENGTH = 9;

  public enum NutType {
    NORMAL,
    EXPLODE_O_NUT,
    GIANT
  }

  public static final class LaneZombie {
    private final int lane;
    private int column;
    private int health;

    private LaneZombie(int lane, int column, int health) {
      this.lane = lane;
      this.column = column;
      this.health = health;
    }

    public int getLane() {
      return lane;
    }

    public int getColumn() {
      return column;
    }

    public int getHealth() {
      return health;
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

    private RollingNut(NutType type, int lane) {
      this.type = type;
      this.lane = lane;
      this.column = 0;
      this.dLane = 0;
      this.dColumn = 1;
    }
  }

  private final List<LaneZombie> zombies = new ArrayList<>();
  private final List<RollingNut> activeNuts = new ArrayList<>();
  private final Random random;
  private final int level;
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
    this.zombiesRemainingToSpawn = 8 + level * 4;
    this.ticksBetweenSpawns = Math.max(15, 45 - level * 10);
    spawnWave(2 + level);
  }

  private void spawnWave(int count) {
    for (int i = 0; i < count && zombiesRemainingToSpawn > 0; i++) {
      int lane = random.nextInt(LANES);
      int health = 60 + level * 25;
      zombies.add(new LaneZombie(lane, LANE_LENGTH - 1, health));
      zombiesRemainingToSpawn--;
    }
  }

  public NutType nextAvailableNutType(NutType requested) {
    if (requested == NutType.EXPLODE_O_NUT && level < 2) {
      return NutType.NORMAL;
    }
    if (requested == NutType.GIANT && level < 3) {
      return NutType.NORMAL;
    }
    return requested;
  }

  public String rollWalnut(int lane, NutType requestedType) {
    if (lane < 0 || lane >= LANES) {
      return "error: lane out of bounds (1-" + LANES + ")";
    }
    NutType type = nextAvailableNutType(requestedType);
    String note = type != requestedType
            ? " (" + requestedType + " isn't unlocked yet at this level; rolling a normal nut instead)"
            : "";
    activeNuts.add(new RollingNut(type, lane));
    return "Rolling a " + type + " down lane " + (lane + 1) + "..." + note;
  }

  public void tick() {
    if (won || lost) {
      return;
    }

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
    zombies.removeIf(LaneZombie::isDead);

    if (checkLoseCondition()) {
      lost = true;
      return;
    }

    if (zombiesRemainingToSpawn <= 0 && zombies.isEmpty()) {
      won = true;
    }
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
      // Hit the row boundary: bounce off it by reflecting the vertical component.
      nut.dLane = -nut.dLane;
      nextLane = nut.lane + nut.dLane;
    }
    nut.lane = Math.max(0, Math.min(LANES - 1, nextLane));
    nut.column += nut.dColumn;

    if (nut.column < 0 || nut.column >= LANE_LENGTH) {
      // Rolled off the far end of the lane without anything left to hit.
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
    hit.health -= 1_000;
    score += 10;
    rotateTrajectory(nut);
  }


  private void rotateTrajectory(RollingNut nut) {
    if (nut.bounceStage == 0) {
      nut.bounceStage = 1;
      nut.dLane = nut.lane <= 0 ? 1 : (nut.lane >= LANES - 1 ? -1 : 1);
    } else {
      nut.bounceStage = 2;
      nut.dColumn = 0;
      if (nut.dLane == 0) {
        nut.dLane = 1;
      }
    }
  }

  private void handleExplodeNutHit(RollingNut nut, LaneZombie hit) {
    for (LaneZombie zombie : zombies) {
      boolean inBlastLane = Math.abs(zombie.lane - nut.lane) <= 1;
      boolean inBlastColumn = Math.abs(zombie.column - nut.column) <= 1;
      if (inBlastLane && inBlastColumn) {
        zombie.health -= 1_800;
      }
    }
    score += 60;
    nut.spent = true;
  }

  private void handleGiantNutHit(LaneZombie hit) {
    hit.health = 0;
    score += 25;
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

  public String renderMap() {
    StringBuilder sb = new StringBuilder();
    sb.append("--- Wall-nut Bowling: Level ").append(level).append(" | Score: ").append(score)
            .append(" | Zombies left to spawn: ").append(zombiesRemainingToSpawn).append(" ---\n");
    for (int lane = 0; lane < LANES; lane++) {
      StringBuilder row = new StringBuilder();
      for (int col = 0; col < LANE_LENGTH; col++) {
        char glyph = '.';
        LaneZombie z = findZombieAt(lane, col);
        if (z != null) {
          glyph = 'Z';
        }
        for (RollingNut nut : activeNuts) {
          if (!nut.spent && nut.lane == lane && nut.column == col) {
            glyph = 'O';
              break;
          }
        }
        row.append('[').append(glyph).append(']');
      }
      sb.append("Lane ").append(lane + 1).append(": ").append(row).append('\n');
    }
    return sb.toString();
  }
}