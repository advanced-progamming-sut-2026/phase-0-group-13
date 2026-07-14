package model.enums;

public enum ScoreEvent {
  MULTI_KILL_ONE_SHOT(50),

  KILL_FAST_ZOMBIE(30),

  SIMULTANEOUS_KILL(40),

  WAVE_CLEARED_NO_LOSS(100),

  SPEED_SUN_COLLECT(20);

  private final int points;

  ScoreEvent(int points) {
    this.points = points;
  }

  public int getPoints() {
    return points;
  }
}
