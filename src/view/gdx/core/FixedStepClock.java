package view.gdx.core;


public final class FixedStepClock {

  /** Cap on catch-up steps, otherwise one long freeze traps us in the loop. */
  private static final int MAX_STEPS_PER_FRAME = 5;

  private final float stepSeconds;
  private float accumulator;

  public FixedStepClock(float stepSeconds) {
    this.stepSeconds = stepSeconds;
  }

  /**
   * Runs step once for every full interval that has passed.
   *
   * @param deltaSeconds frame delta, usually Gdx.graphics.getDeltaTime()
   * @param step what to run
   * @return how many steps actually ran
   */
  public int update(float deltaSeconds, Runnable step) {
    accumulator += deltaSeconds;
    int steps = 0;
    while (accumulator >= stepSeconds && steps < MAX_STEPS_PER_FRAME) {
      accumulator -= stepSeconds;
      steps++;
      step.run();
    }
    if (steps == MAX_STEPS_PER_FRAME) {
      accumulator = 0f;
    }
    return steps;
  }

  public float alpha() {
    return accumulator / stepSeconds;
  }

  public void reset() {
    accumulator = 0f;
  }
}
