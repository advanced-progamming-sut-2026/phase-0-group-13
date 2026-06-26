package model.game.TileEffects;

public class TileEffect {
  private String name;
  private int duration;
  private boolean active;

  public TileEffect(String name, int duration) {
    this.name = name;
    this.duration = duration;
    this.active = true;
  }

  public void apply() {
    this.active = true;
  }

  public void tick() {
    if (!active) return;

    if (duration > 0) {
      duration--;
      if (duration == 0) {
        remove();
      }
    }
  }

  public void remove() {
    this.active = false;
  }

  public String getName() {
    return name;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
