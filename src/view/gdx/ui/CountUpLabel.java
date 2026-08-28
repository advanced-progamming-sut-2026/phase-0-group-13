package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * A number that runs up to its value instead of appearing at it.
 *
 * <p>Only for the end-of-level rewards, where the number is the point of the screen: watching a
 * coin total climb is what makes a reward feel earned rather than reported. It is deliberately
 * short and it always finishes -- the final value is set the moment the roll is over, so a player
 * who reads the panel late still sees the right number, and nothing else waits on it.
 */
public final class CountUpLabel extends Label {

  /** How long the roll takes, however large the number is. */
  private static final float SECONDS = 0.7f;

  private final int target;
  private final String suffix;
  private float elapsed;
  private boolean done;

  public CountUpLabel(int target, Skin skin, String style) {
    this(target, "", skin, style);
  }

  public CountUpLabel(int target, String suffix, Skin skin, String style) {
    super("0" + suffix, skin, style);
    this.target = Math.max(0, target);
    this.suffix = suffix;
    if (this.target == 0) {
      finish();
    }
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (done) {
      return;
    }
    elapsed += delta;
    if (elapsed >= SECONDS) {
      finish();
      return;
    }
    // Eases out, so it slows as it lands rather than stopping dead.
    float progress = elapsed / SECONDS;
    float eased = 1f - (1f - progress) * (1f - progress);
    setText(Math.round(target * eased) + suffix);
  }

  private void finish() {
    done = true;
    setText(target + suffix);
  }
}
