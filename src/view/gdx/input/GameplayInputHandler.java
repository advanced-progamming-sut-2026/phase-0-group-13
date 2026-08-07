package view.gdx.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import view.gdx.render.LawnGeometry;
import view.gdx.render.RenderContext;


/**
 * Turns clicks and key presses into GameActionBridge calls.
 *
 * <p>Sits under the HUD in an InputMultiplexer, so Scene2D sees a click first and buttons win.
 * Whatever it doesn't take falls through to the lawn.
 *
 * <p>The unprojecting and the tile lookup are done, but nothing gets dispatched until
 * GameActionBridge has an implementation.
 */
public final class GameplayInputHandler extends InputAdapter {

  private final RenderContext context;
  private final LawnGeometry geometry;
  private final GameActionBridge actions;

  /** Reused so we don't make a new Vector2 on every event. */
  private final Vector2 scratch = new Vector2();

  /** Template name of the seed the player picked, or null. */
  private String selectedPlantType;

  public GameplayInputHandler(
      RenderContext context, LawnGeometry geometry, GameActionBridge actions) {
    this.context = context;
    this.geometry = geometry;
    this.actions = actions;
  }

  public void setSelectedPlantType(String plantType) {
    this.selectedPlantType = plantType;
  }

  public String getSelectedPlantType() {
    return selectedPlantType;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    scratch.set(screenX, screenY);
    context.getViewport().unproject(scratch);

    int row = geometry.yToRow(scratch.y);
    int column = geometry.xToColumn(scratch.x);
    if (row < 0 || column < 0) {
      return false;
    }

    // TODO if there's a sun here, actions.collectSunAt(row, column) and take the click
    // TODO else if a seed is picked, actions.plantAt(row, column, selectedPlantType)
    // TODO else if the shovel is on, actions.pluckAt(row, column)
    // TODO else if plant food is armed, actions.feedPlantAt(row, column)
    return false;
  }

  @Override
  public boolean keyDown(int keycode) {
    // TODO escape quits, number keys pick a seed, space pauses
    return false;
  }
}
