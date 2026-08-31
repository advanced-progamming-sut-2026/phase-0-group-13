package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public final class CountUpLabel extends Label {

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
    float progress = elapsed / SECONDS;
    float eased = 1f - (1f - progress) * (1f - progress);
    setText(Math.round(target * eased) + suffix);
  }

  private void finish() {
    done = true;
    setText(target + suffix);
  }
}
