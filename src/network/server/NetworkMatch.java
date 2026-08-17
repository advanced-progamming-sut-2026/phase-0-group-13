package network.server;

import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieMatch;

public final class NetworkMatch {

  private final String id;
  private final String plantsPlayer;
  private final String zombiesPlayer;
  private final IZombieMatch state;
  private volatile boolean finished;

  public NetworkMatch(String id, String plantsPlayer, String zombiesPlayer, int level, long seed) {
    this.id = id;
    this.plantsPlayer = plantsPlayer;
    this.zombiesPlayer = zombiesPlayer;
    this.state = new IZombieMatch(level, seed);
  }

  public String getId() {
    return id;
  }

  public IZombieMatch getState() {
    return state;
  }

  public MatchRole roleOf(String username) {
    if (plantsPlayer.equalsIgnoreCase(username)) {
      return MatchRole.PLANTS;
    }
    return zombiesPlayer.equalsIgnoreCase(username) ? MatchRole.ZOMBIES : null;
  }

  public String opponentOf(String username) {
    if (plantsPlayer.equalsIgnoreCase(username)) {
      return zombiesPlayer;
    }
    return zombiesPlayer.equalsIgnoreCase(username) ? plantsPlayer : null;
  }

  public String playerOf(MatchRole role) {
    return role == MatchRole.PLANTS ? plantsPlayer : zombiesPlayer;
  }

  public boolean isFinished() {
    return finished || state.isFinished();
  }

  public void markFinished() {
    this.finished = true;
  }
}
