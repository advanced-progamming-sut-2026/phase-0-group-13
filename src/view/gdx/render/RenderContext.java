package view.gdx.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import view.gdx.core.GdxConfig;


/**
 * The drawing stuff every renderer needs: one batch, one shape renderer, one camera, one viewport.
 *
 * <p>The game owns it and hands it to the screens, so we never make a SpriteBatch per screen.
 * That is the usual way to leak native memory in a libGDX project. Build it only after the GL
 * context exists, so from create() onwards.
 */
public final class RenderContext implements Disposable {

  private final SpriteBatch batch;
  private final ShapeRenderer shapes;
  private final OrthographicCamera camera;
  private final Viewport viewport;

  public RenderContext() {
    this.batch = new SpriteBatch();
    this.shapes = new ShapeRenderer();
    this.camera = new OrthographicCamera();
    this.viewport = new FitViewport(GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT, camera);
    this.viewport.apply(true);
  }

  public SpriteBatch getBatch() {
    return batch;
  }

  public ShapeRenderer getShapes() {
    return shapes;
  }

  public OrthographicCamera getCamera() {
    return camera;
  }

  public Viewport getViewport() {
    return viewport;
  }

  /** Points the batch and shapes at the camera. Call once a frame before drawing. */
  public void applyCamera() {
    camera.update();
    batch.setProjectionMatrix(camera.combined);
    shapes.setProjectionMatrix(camera.combined);
  }

  /** Passes a window resize on to the viewport. Screens get here through BaseScreen.resize. */
  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    batch.dispose();
    shapes.dispose();
  }
}
