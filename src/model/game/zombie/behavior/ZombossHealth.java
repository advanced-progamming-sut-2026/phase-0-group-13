package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.List;

/**
 * جان زامباس، تقسیم‌شده به سه بخش. اندازهٔ هر بخش از Stages[].HitPoints خودِ Zombies.json می‌آید،
 * پس باسی که فاز میانی‌اش سنگین‌تر است همان‌طور هم می‌ماند.
 *
 * <p>Holds no state of its own: every answer is worked out from the boss's current health, so the
 * bar the HUD draws and the stun the behaviour fires can never drift apart.
 */
public final class ZombossHealth {

  public static final int SEGMENTS = 3;

  /** Segment 0 empties first. */
  private final int[] capacity;
  private final int total;

  public ZombossHealth(List<Integer> stageHitPoints, int totalHealth) {
    this.total = Math.max(SEGMENTS, totalHealth);
    this.capacity = split(stageHitPoints, this.total);
  }

  public int getTotal() {
    return total;
  }

  public int capacityOf(int segment) {
    return segment < 0 || segment >= SEGMENTS ? 0 : capacity[segment];
  }

  /** How much of one segment is still standing at this health. */
  public int remainingOf(int segment, int currentHealth) {
    if (segment < 0 || segment >= SEGMENTS) {
      return 0;
    }
    int spent = Math.max(0, total - Math.max(0, currentHealth));
    for (int i = 0; i < segment; i++) {
      spent -= capacity[i];
    }
    return Math.max(0, Math.min(capacity[segment], capacity[segment] - spent));
  }

  public float fractionOf(int segment, int currentHealth) {
    int room = capacityOf(segment);
    return room <= 0 ? 0f : remainingOf(segment, currentHealth) / (float) room;
  }

  /** How many segments are empty. Three means the boss is down. */
  public int segmentsCleared(int currentHealth) {
    int cleared = 0;
    for (int i = 0; i < SEGMENTS; i++) {
      if (remainingOf(i, currentHealth) <= 0) {
        cleared++;
      }
    }
    return cleared;
  }

  public int segmentsLeft(int currentHealth) {
    return SEGMENTS - segmentsCleared(currentHealth);
  }

  /**
   * The three chunk sizes, always adding up to exactly {@code total}.
   *
   * <p>The raw stage numbers cannot be used as they stand: the factory scales a zombie's health by
   * the difficulty, so the sheet's 4000/8000/6500 would no longer match the health the boss
   * actually has. They are rescaled to the real total instead, and a boss whose sheet does not
   * carry three stages just gets even thirds.
   */
  private static int[] split(List<Integer> stageHitPoints, int total) {
    List<Integer> stages = new ArrayList<>();
    if (stageHitPoints != null) {
      for (Integer hp : stageHitPoints) {
        if (hp != null && hp > 0) {
          stages.add(hp);
        }
      }
    }
    if (stages.size() != SEGMENTS) {
      return evenThirds(total);
    }
    long sum = 0;
    for (int hp : stages) {
      sum += hp;
    }
    int[] result = new int[SEGMENTS];
    int assigned = 0;
    for (int i = 0; i < SEGMENTS - 1; i++) {
      result[i] = Math.max(1, (int) Math.round(stages.get(i) * (double) total / sum));
      assigned += result[i];
    }
    result[SEGMENTS - 1] = total - assigned;
    return result[SEGMENTS - 1] < 1 ? evenThirds(total) : result;
  }

  private static int[] evenThirds(int total) {
    int each = total / SEGMENTS;
    return new int[] {each, each, total - 2 * each};
  }
}
