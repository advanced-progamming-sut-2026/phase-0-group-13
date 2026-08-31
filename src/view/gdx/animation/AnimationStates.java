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

  /**
   * The current playback time for an entity that has already been advanced this frame, without
   * moving it. For a caller that needs to know exactly what pose is on screen right now -- the
   * health bar reading where a mid-frame limb actually is -- without itself driving the clip
   * forward, which advance() always does and calling it twice in one frame would double.
   *
   * @return the stored time, or 0 if this entity has not been advanced yet this frame
   */
  public float peek(Object entity) {
    State state = states.get(entity);
    return state == null ? 0f : state.time;
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
