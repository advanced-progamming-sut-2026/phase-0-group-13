package view.gdx.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Draws several drawables one over another in the same box.
 *
 * <p>Scene2D gives an actor exactly one background, but a panel that reads as raised needs two: a
 * flat fill, and the dialog frame over the top of it. The frame on its own is a border with a hole
 * in the middle, so using it alone leaves the panel transparent and whatever text is on it lands on
 * the artwork behind.
 *
 * <p>Padding comes from the first layer that declares any, which is the frame in practice -- its
 * insets are the ones that keep content off the border.
 */
public final class LayeredDrawable extends BaseDrawable {

  private final Drawable[] layers;

  /** Bottom layer first. */
  public LayeredDrawable(Drawable... layers) {
    this.layers = layers;
    for (Drawable layer : layers) {
      setMinWidth(Math.max(getMinWidth(), layer.getMinWidth()));
      setMinHeight(Math.max(getMinHeight(), layer.getMinHeight()));
      if (getLeftWidth() == 0f && layer.getLeftWidth() > 0f) {
        setLeftWidth(layer.getLeftWidth());
        setRightWidth(layer.getRightWidth());
        setTopHeight(layer.getTopHeight());
        setBottomHeight(layer.getBottomHeight());
      }
    }
  }

  @Override
  public void draw(Batch batch, float x, float y, float width, float height) {
    for (Drawable layer : layers) {
      layer.draw(batch, x, y, width, height);
    }
  }
}
