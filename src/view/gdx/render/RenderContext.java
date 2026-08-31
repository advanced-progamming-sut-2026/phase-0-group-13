package view.gdx.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import view.gdx.core.GdxConfig;


public final class RenderContext implements Disposable {

  private final SpriteBatch batch;
  private final ShapeRenderer shapes;
  private final OrthographicCamera camera;
  private final Viewport viewport;

  public RenderContext() {
    this.batch = new SpriteBatch();
    this.shapes = new ShapeRenderer();
    this.camera = new OrthographicCamera();
    this.viewport = new FillViewport(GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT, camera);
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

  public void applyCamera() {
    camera.update();
    batch.setProjectionMatrix(camera.combined);
    shapes.setProjectionMatrix(camera.combined);
  }

  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    batch.dispose();
    shapes.dispose();
  }
}
