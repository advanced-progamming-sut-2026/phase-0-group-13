package view.gdx.core;


/**
 * Turns the frame delta into a fixed number of simulation steps.
 *
 * <p>The model moves in ticks (GameManager.advanceTime()) but the render loop runs as fast as it
 * can, so we save up the leftover time and run a step once enough has built up.
 *
 * <p>Takes a Runnable instead of a GameManager so the simulation stays out of the view.
 */
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

  /** How far into the next step we are, 0 to 1. Renderers can use it to smooth movement. */
  public float alpha() {
    return accumulator / stepSeconds;
  }

  /** Throws away the leftover time. Call it when a match starts or after a pause. */
  public void reset() {
    accumulator = 0f;
  }
}
