package view.gdx.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import java.util.List;
import view.gdx.render.LawnGeometry;
import view.gdx.render.RenderContext;

/**
 * Turns lawn clicks and a few keys into {@link GameActionBridge} calls.
 *
 * <p>Only one of a selected seed, the shovel or plant food is armed at a time, same as the real
 * game: picking one clears the others. A click either uses whichever is armed, or falls back to
 * collectSunAt (harmless if there is no sun on that tile).
 */
public final class GameplayInputHandler extends InputAdapter {
  private final RenderContext context;
  private final LawnGeometry geometry;
  private final GameActionBridge actions;
  private final Vector2 scratch = new Vector2();

  private String selectedPlantType;
  private boolean shovelArmed;
  private boolean plantFoodArmed;
  private List<String> deck = List.of();
  private Runnable onPauseRequested;
  private boolean paused;

  public GameplayInputHandler(RenderContext context, LawnGeometry geometry, GameActionBridge actions) {
    this.context = context;
    this.geometry = geometry;
    this.actions = actions;
  }

  public void setSelectedPlantType(String plantType) {
    this.selectedPlantType = plantType;
    this.shovelArmed = false;
    this.plantFoodArmed = false;
  }

  public String getSelectedPlantType() {
    return selectedPlantType;
  }

  /** Arms/disarms the shovel; a second click on the shovel button cancels it. */
  public void armShovel() {
    shovelArmed = !shovelArmed;
    if (shovelArmed) {
      plantFoodArmed = false;
      selectedPlantType = null;
    }
  }

  public boolean isShovelArmed() {
    return shovelArmed;
  }

  /** Arms/disarms plant food; a second click on the button cancels it. */
  public void armPlantFood() {
    plantFoodArmed = !plantFoodArmed;
    if (plantFoodArmed) {
      shovelArmed = false;
      selectedPlantType = null;
    }
  }

  public boolean isPlantFoodArmed() {
    return plantFoodArmed;
  }

  /** The seed bank in bar order, so number keys can pick a seed by position. */
  public void setDeck(List<String> deck) {
    this.deck = deck == null ? List.of() : deck;
  }

  /** What Escape and Space do; GameplayScreen wires this to opening the pause menu. */
  public void setOnPauseRequested(Runnable onPauseRequested) {
    this.onPauseRequested = onPauseRequested;
  }

  /** While paused, clicks and keys should not reach the match. */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    if (paused) {
      return false;
    }
    scratch.set(screenX, screenY);
    context.getViewport().unproject(scratch);
    int row = geometry.yToRow(scratch.y);
    int column = geometry.xToColumn(scratch.x);
    if (row < 0 || column < 0) {
      return false;
    }

    if (shovelArmed) {
      actions.pluckAt(row, column);
      shovelArmed = false;
      return true;
    }
    if (plantFoodArmed) {
      actions.feedPlantAt(row, column);
      plantFoodArmed = false;
      return true;
    }
    if (selectedPlantType != null) {
      actions.plantAt(row, column, selectedPlantType);
      return true;
    }
    actions.collectSunAt(row, column);
    return true;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (paused) {
      return false;
    }
    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.SPACE) {
      if (onPauseRequested != null) {
        onPauseRequested.run();
      }
      return true;
    }
    int index = numberKeyIndex(keycode);
    if (index >= 0 && index < deck.size()) {
      setSelectedPlantType(deck.get(index));
      return true;
    }
    return false;
  }

  private static int numberKeyIndex(int keycode) {
    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
      return keycode - Input.Keys.NUM_1;
    }
    return -1;
  }
}
