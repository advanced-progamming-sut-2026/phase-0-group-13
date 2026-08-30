package model.game.TileEffects;

/**
 * زمینِ آتش‌گرفته. آتشِ گلولهٔ اژدهای دوران تاریکی خانه را می‌سوزاند: تا وقتی شعله برپاست هرچه
 * روی خانه است می‌سوزد و جای خالی هم قابل کاشت نیست.
 */
public class FireEffect extends TileEffect {

  /** How hard the flames hit whatever is standing in them, per tick. */
  public static final int DAMAGE_PER_TICK = 40;

  public FireEffect(int durationTicks) {
    super("Fire", durationTicks);
  }

  @Override
  public boolean blocksPlanting() {
    return isActive();
  }
}
