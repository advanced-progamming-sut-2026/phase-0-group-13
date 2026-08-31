package model.game.zombie.behavior;

import java.util.ArrayList;
import java.util.List;

public final class ZombossHealth {

  public static final int SEGMENTS = 3;

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
