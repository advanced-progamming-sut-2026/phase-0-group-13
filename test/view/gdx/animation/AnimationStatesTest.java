package view.gdx.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * peek() has to be read-only. It exists so a second consumer of a clip's playback this frame --
 * the health bar reading where the sprite {@link #advance} already drew it to -- can ask "where is
 * this entity in its clip right now" without itself moving the clip forward. Calling advance()
 * again for the same purpose would add its delta a second time, which would not break anything
 * loudly: it would just make every zombie's walk cycle play twice as fast as its actual movement,
 * with nothing in the game ever reporting it.
 */
class AnimationStatesTest {

  @Test
  void peekReturnsWhatTheLastAdvanceLeftBehind() {
    AnimationStates states = new AnimationStates();
    Object zombie = new Object();
    float time = states.advance(zombie, "walk", 0.4f);

    assertEquals(time, states.peek(zombie), 1e-6f);
  }

  @Test
  void peekingDoesNotMoveTheClipForward() {
    AnimationStates states = new AnimationStates();
    Object zombie = new Object();
    states.advance(zombie, "walk", 0.4f);

    float first = states.peek(zombie);
    float second = states.peek(zombie);
    float third = states.peek(zombie);

    assertEquals(first, second, 1e-6f);
    assertEquals(second, third, 1e-6f);
  }

  @Test
  void repeatedPeeksNeverAccumulateLikeRepeatedAdvancesWould() {
    AnimationStates states = new AnimationStates();
    Object zombie = new Object();
    states.advance(zombie, "walk", 0.1f);

    for (int i = 0; i < 50; i++) {
      states.peek(zombie);
    }
    assertEquals(0.1f, states.peek(zombie), 1e-6f,
        "fifty peeks moved the clip; peek() is supposed to be side-effect free");

    // Contrast: this is what fifty *advances* would have done, which is the bug peek() exists
    // to avoid a caller falling into.
    for (int i = 0; i < 50; i++) {
      states.advance(zombie, "walk", 0.1f);
    }
    assertNotEquals(0.1f, states.peek(zombie), 1e-6f);
  }

  @Test
  void anEntityNeverAdvancedPeeksAsZero() {
    AnimationStates states = new AnimationStates();
    assertEquals(0f, states.peek(new Object()), 1e-6f);
  }
}
