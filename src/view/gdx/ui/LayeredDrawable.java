package view.gdx.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public final class LayeredDrawable extends BaseDrawable {

  private final Drawable[] layers;

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
