package model.game.minigame.arcade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.enums.MatchRole;

/**
 * Two-sided match around {@link IZombieEngine}. The seed is explicit so the server and both
 * clients can build the same starting board.
 *
 * <p>Both players act through {@link #apply}, which is the only door into the engine: it checks
 * who is allowed to make this kind of move and then hands the move straight to the engine, whose
 * refusal is passed back word for word. No rule is decided here and none is decided again in a
 * client -- the engine owns the price, the recharge, the red line and what is already on the tile.
 *
 * <p>The match ends one of three ways: the attacker eats all five brains, the defender survives
 * {@link #SURVIVAL_SECONDS}, or the engine declares the attacker unable to act. The first to
 * happen wins, and once a winner is decided it never changes, which is what lets the same verdict
 * be delivered to two clients.
 */
public final class IZombieMatch {

  /** The rate every cadence in {@link IZombieEngine} is written in. */
  public static final int TICK_MILLIS = 1000 / IZombieEngine.TICKS_PER_SECOND;

  /** How long the defending player has to hold out to win. */
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

  /**
   * Brains first, so an attacker who eats the last one on the very tick the clock runs out still
   * wins it. Nothing is re-decided once a winner is set.
   */
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

  /** Null while the match is still running. */
  public MatchRole winner() {
    return winner;
  }

  /**
   * Everything a client needs to draw the board, so neither of them has to run a second copy of
   * the game to fill in what the protocol left out.
   */
  public Snapshot snapshot() {
    List<PlantView> plants = new ArrayList<>();
    for (IZombieEngine.DefensePlant plant : engine.getDefensePlants()) {
      plants.add(new PlantView(plant.getId(), plant.getName(), plant.getRow(), plant.getCol(),
          plant.getHealth(), plant.getMaxHealth()));
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
        engine.getBrainsRemaining(), brains, isFinished(), winner, plants, zombies,
        zombieRecharge, plantRecharge);
  }

  /**
   * What a side is worth at the end of a match, so the two clients and the server all arrive at
   * the same number from the same board: the attacker is measured in brains taken, the defender in
   * seconds held.
   */
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

  public record PlantView(int id, String name, int row, int col, int health, int maxHealth) {}

  /** Column is unrounded so a client draws the walk rather than a jump per cell. */
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
      Map<String, Integer> zombieRecharge,
      Map<String, Integer> plantRecharge) {}
}
