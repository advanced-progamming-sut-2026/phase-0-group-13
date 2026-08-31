package view.gdx.render;

import com.badlogic.gdx.utils.Disposable;
import model.core.GameManager;


public interface WorldRenderer extends Disposable {

  /**
   * Draws one frame.
   *
   * @param context shared batch, shapes and camera
   * @param game the match to draw, can be null before one starts
   * @param delta frame delta, for animation only, never for the simulation
   */
  void render(RenderContext context, GameManager game, float delta);
}
