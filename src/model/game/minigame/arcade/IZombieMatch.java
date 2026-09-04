package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.enums.MatchRole;

public final class IZombieMatch {

  public static final int TICK_MILLIS = 1000 / IZombieEngine.TICKS_PER_SECOND;

  public static final int SURVIVAL_SECONDS = 120;
  public static final int SURVIVAL_TICKS = SURVIVAL_SECONDS * IZombieEngine.TICKS_PER_SECOND;

  private static final int POINTS_PER_BRAIN = 100;
  private static final int POINTS_PER_SECOND_HELD = 4;

  private final IZombieEngine engine;
  private int tick;
  private MatchRole winner;

  public IZombieMatch(int level, long seed) {
    this.engine = new IZombieEngine(level, new Random(seed));
  }

  /** @return null when applied, otherwise the reason it was rejected */
  public String apply(MatchRole actor, IZombieAction action) {
    if (action == null) {
      return "error: empty action";
    }
    if (isFinished()) {
      return "error: the match is over";
    }
    return switch (action.kind()) {
      case PLACE_ZOMBIE -> actor == MatchRole.ZOMBIES
          ? engineResult(engine.placeZombie(action.type(), action.row(), action.col()))
          : "error: only the zombie player can place zombies";
      case PLACE_PLANT -> actor == MatchRole.PLANTS
          ? engineResult(engine.placePlant(action.type(), action.row(), action.col()))
          : "error: only the plant player can place plants";
    };
  }

  private static String engineResult(String engineMessage) {
    return engineMessage != null && engineMessage.startsWith("error:") ? engineMessage : null;
  }

  public void tick() {
    if (isFinished()) {
      return;
    }
    engine.tick();
    tick++;
    decide();
  }

  private void decide() {
    if (winner != null) {
      return;
    }
    if (engine.isWon()) {
      winner = MatchRole.ZOMBIES;
    } else if (engine.isLost() || tick >= SURVIVAL_TICKS) {
      winner = MatchRole.PLANTS;
    }
  }

  public int getTick() {
    return tick;
  }

  public int ticksRemaining() {
    return Math.max(0, SURVIVAL_TICKS - tick);
  }

  public IZombieEngine getEngine() {
    return engine;
  }

  public boolean isFinished() {
    return winner != null;
  }

  public MatchRole winner() {
    return winner;
  }

  public Snapshot snapshot() {
    List<PlantView> plants = new ArrayList<>();
    for (IZombieEngine.DefensePlant plant : engine.getDefensePlants()) {
      plants.add(new PlantView(plant.getId(), plant.getName(), plant.getRow(), plant.getCol(),
          plant.getHealth(), plant.getMaxHealth(), engine.ticksToShot(plant)));
    }
    List<ZombieView> zombies = new ArrayList<>();
    for (IZombieEngine.DeployedZombie zombie : engine.getDeployedZombies()) {
      if (zombie.isDead()) {
        continue;
      }
      zombies.add(new ZombieView(zombie.getId(), zombie.getName(), zombie.getRow(),
          zombie.getColumn(), zombie.getHealth(), zombie.getMaxHealth(), zombie.isEating(),
          zombie.producesSun()));
    }
    List<ShotView> shots = new ArrayList<>();
    for (IZombieEngine.Shot shot : engine.getShots()) {
      shots.add(new ShotView(shot.getRow(), shot.getColumn()));
    }
    boolean[] brains = new boolean[IZombieEngine.BRAINS];
    for (int row = 0; row < brains.length; row++) {
      brains[row] = engine.isBrainAlive(row);
    }
    Map<String, Integer> zombieRecharge = new LinkedHashMap<>();
    for (IZombieEngine.ZombieSpec spec : engine.availableZombieTypes()) {
      zombieRecharge.put(spec.name, engine.rechargeTicksLeft(spec.name));
    }
    Map<String, Integer> plantRecharge = new LinkedHashMap<>();
    for (IZombieEngine.PlantSpec spec : IZombieEngine.availablePlantTypes()) {
      plantRecharge.put(spec.name, engine.plantRechargeTicksLeft(spec.name));
    }
    return new Snapshot(tick, ticksRemaining(), engine.getZombieSun(), engine.getPlantSun(),
        engine.getBrainsRemaining(), brains, isFinished(), winner, plants, zombies, shots,
        zombieRecharge, plantRecharge);
  }

  public static int scoreFor(Snapshot snapshot, MatchRole role) {
    if (snapshot == null || role == null) {
      return 0;
    }
    if (role == MatchRole.ZOMBIES) {
      return (IZombieEngine.BRAINS - snapshot.brainsRemaining()) * POINTS_PER_BRAIN;
    }
    int held = Math.min(snapshot.tick(), SURVIVAL_TICKS) / IZombieEngine.TICKS_PER_SECOND;
    return held * POINTS_PER_SECOND_HELD;
  }

  /** @param ticksToShot ticks until this cutout's next pea, or -1 for one that banks sun instead */
  public record PlantView(int id, String name, int row, int col, int health, int maxHealth,
      int ticksToShot) {}

  /** A pea in flight, so the screens can draw the shot that is taking the health off a zombie. */
  public record ShotView(int row, double column) {}

  public record ZombieView(
      int id,
      String type,
      int row,
      double column,
      int health,
      int maxHealth,
      boolean eating,
      boolean producesSun) {}

  public record Snapshot(
      int tick,
      int ticksRemaining,
      int zombieSun,
      int plantSun,
      int brainsRemaining,
      boolean[] brains,
      boolean finished,
      MatchRole winner,
      List<PlantView> plants,
      List<ZombieView> zombies,
      List<ShotView> shots,
      Map<String, Integer> zombieRecharge,
      Map<String, Integer> plantRecharge) {}
}
