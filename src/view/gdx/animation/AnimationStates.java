package view.gdx.animation;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Keeps every entity's place in whatever clip it is currently playing.
 *
 * <p>Playback has to belong to the entity rather than to the animation: two zombies of the same
 * kind share one {@link EntityAnimation}, but one of them can be walking while the other eats.
 * Asking for a different clip than last frame restarts the timer, so a zombie that reaches a plant
 * starts its eat cycle from the beginning instead of halfway through.
 *
 * <p>Keyed by identity and swept once a frame, so nothing is held onto after a zombie dies.
 */
public final class AnimationStates {

  private static final class State {
    String clip;
    float time;
    int seenOn;
  }

  private final Map<Object, State> states = new IdentityHashMap<>();
  private int frame;

  /**
   * Moves this entity along its clip and gives back how far into it we are.
   *
   * @param delta seconds to add, zero to hold the current frame
   */
  public float advance(Object entity, String clip, float delta) {
    State state = states.get(entity);
    if (state == null) {
      state = new State();
      states.put(entity, state);
    }
    if (!clip.equals(state.clip)) {
      state.clip = clip;
      state.time = 0f;
    }
    state.time += Math.max(0f, delta);
    state.seenOn = frame;
    return state.time;
  }

  /** Forgets anything that was not drawn this frame. */
  public void endFrame() {
    Iterator<State> it = states.values().iterator();
    while (it.hasNext()) {
      if (it.next().seenOn != frame) {
        it.remove();
      }
    }
    frame++;
  }
}
