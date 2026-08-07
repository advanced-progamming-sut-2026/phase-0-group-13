package view.gdx.render;

import com.badlogic.gdx.utils.Disposable;
import model.core.GameManager;


/**
 * Anything that draws match state in world space.
 *
 * <p>Same shape as view.BoardRenderer.render(GameManager) in the terminal version: read the
 * manager, draw it, don't change it.
 *
 * <p>The HUD goes through Scene2D instead (see HudStage), so this is only for the world.
 */
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
