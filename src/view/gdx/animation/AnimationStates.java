package view.gdx.animation;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

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
