package model.game.minigame.arcade;

import java.util.Random;
import model.enums.MatchRole;

/**
 * Two-sided match around {@link IZombieEngine}. The seed is explicit so the server and both
 * clients can build the same starting board.
 */
public final class IZombieMatch {

  private final IZombieEngine engine;
  private int tick;

  public IZombieMatch(int level, long seed) {
    this.engine = new IZombieEngine(level, new Random(seed));
  }

  /** @return null when applied, otherwise the reason it was rejected */
  public String apply(MatchRole actor, IZombieAction action) {
    if (action == null) {
      return "error: empty action";
    }
    if (engine.isFinished()) {
      return "error: the match is over";
    }
    return switch (action.kind()) {
      case PLACE_ZOMBIE -> actor == MatchRole.ZOMBIES
          ? zombieResult(engine.placeZombie(action.type(), action.row(), action.col()))
          : "error: only the zombie player can place zombies";
      case PLACE_PLANT -> actor == MatchRole.PLANTS
          ? "error: plant placement is not implemented yet"
          : "error: only the plant player can place plants";
    };
  }

  private static String zombieResult(String engineMessage) {
    return engineMessage != null && engineMessage.startsWith("error:") ? engineMessage : null;
  }

  public void tick() {
    engine.tick();
    tick++;
  }

  public Snapshot snapshot() {
    int[][] plants = new int[IZombieEngine.ROWS][IZombieEngine.COLS];
    int[][] zombies = new int[IZombieEngine.ROWS][IZombieEngine.COLS];
    for (int row = 0; row < IZombieEngine.ROWS; row++) {
      for (int col = 0; col < IZombieEngine.COLS; col++) {
        plants[row][col] = engine.getPlantHealthAt(row, col);
        zombies[row][col] = engine.getZombieHealthAt(row, col);
      }
    }
    return new Snapshot(tick, engine.getZombieSun(), engine.getBrainsRemaining(),
        engine.isFinished(), engine.isWon(), plants, zombies);
  }

  public boolean isFinished() {
    return engine.isFinished();
  }

  /** Null while the match is still running. */
  public MatchRole winner() {
    if (!engine.isFinished()) {
      return null;
    }
    return engine.isWon() ? MatchRole.ZOMBIES : MatchRole.PLANTS;
  }

  public record Snapshot(
      int tick,
      int zombieSun,
      int brainsRemaining,
      boolean finished,
      boolean zombiesWon,
      int[][] plantHealth,
      int[][] zombieHealth) {}
}
