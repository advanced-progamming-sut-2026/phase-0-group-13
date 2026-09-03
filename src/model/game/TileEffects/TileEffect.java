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

  /**
   * آیا این خانه تا وقتی افکت فعال است غیرقابل کاشت می‌شود؟ طبق داک سنگ‌قبر (و موانعی مثل دبه)
   * غیرقابل کاشت‌اند؛ افکت‌های دیگر به‌صورت پیش‌فرض مانع کاشت نیستند.
   */
  public boolean blocksPlanting() {
    return false;
  }

  /**
   * Whether this effect blocks a particular plant.
   *
   * <p>Two plants in the roster exist only to be planted on a tile that is otherwise unplantable:
   * Grave Buster "destroys gravestones" and Hot Potato is "planted on ice to melt it instantly".
   * Asking the effect rather than special-casing them at the call sites keeps the rule in one
   * place, and keeps it a property of the obstacle rather than of the placement code.
   *
   * @param plantName the plant being placed, or null when the caller is only asking in general
   */
  public boolean blocksPlanting(String plantName) {
    return blocksPlanting() && !clearedBy(plantName);
  }

  /** True for the plant this obstacle is the whole point of, if it has one. */
  protected boolean clearedBy(String plantName) {
    return false;
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
