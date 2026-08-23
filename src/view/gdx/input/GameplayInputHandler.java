package view.gdx.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import view.gdx.render.LawnGeometry;
import view.gdx.render.RenderContext;


/**
 * Turns clicks and key presses into GameActionBridge calls. Sits under the HUD in an
 * InputMultiplexer, so buttons win and the rest falls through to the lawn.
 *
 * <p>Holds the armed tool and the hovered cell, nothing else. No planting rules live here.
 */
public final class GameplayInputHandler extends InputAdapter {

  /** What the next click on the lawn will do. */
  public enum Tool {
    NONE,
    SEED,
    SHOVEL,
    PLANT_FOOD
  }

  private final RenderContext context;
  private final LawnGeometry geometry;
  private final GameActionBridge actions;
  private final Runnable onTogglePause;

  /** Reused so we don't make a new Vector2 on every event. */
  private final Vector2 scratch = new Vector2();

  /** Seed order as the seed bar shows it, so key 1 arms the first card. */
  private final List<String> seedOrder = new ArrayList<>();

  /** Template name of the seed the player picked, or null. */
  private String selectedPlantType;

  private Tool tool = Tool.NONE;
  private int hoverRow = -1;
  private int hoverColumn = -1;
  private float pointerWorldX;
  private float pointerWorldY;
  private boolean paused;

  public GameplayInputHandler(
      RenderContext context,
      LawnGeometry geometry,
      GameActionBridge actions,
      Runnable onTogglePause) {
    this.context = context;
    this.geometry = geometry;
    this.actions = actions;
    this.onTogglePause = onTogglePause;
  }

  /** Null disarms everything. */
  public void setSelectedPlantType(String plantType) {
    this.selectedPlantType = plantType;
    this.tool = plantType == null ? Tool.NONE : Tool.SEED;
  }

  public String getSelectedPlantType() {
    return selectedPlantType;
  }

  public void setSeedOrder(List<String> plants) {
    seedOrder.clear();
    if (plants != null) {
      seedOrder.addAll(plants);
    }
  }

  public void toggleShovel() {
    setTool(Tool.SHOVEL);
  }

  public void togglePlantFood() {
    setTool(Tool.PLANT_FOOD);
  }

  // pressing the armed tool again disarms it

  private void setTool(Tool wanted) {
    boolean alreadyOn = tool == wanted;
    tool = alreadyOn ? Tool.NONE : wanted;
    selectedPlantType = null;
  }

  public void clearTool() {
    tool = Tool.NONE;
    selectedPlantType = null;
  }

  public Tool getTool() {
    return tool;
  }

  /** Frozen game: clicks stop reaching the lawn. */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public int getHoverRow() {
    return hoverRow;
  }

  public int getHoverColumn() {
    return hoverColumn;
  }

  public float getPointerWorldX() {
    return pointerWorldX;
  }

  public float getPointerWorldY() {
    return pointerWorldY;
  }

  public boolean isHoveringLawn() {
    return hoverRow >= 0 && hoverColumn >= 0;
  }

  // The screen calls this every frame, not just from mouseMoved: the HUD is in front in the
  // multiplexer and can swallow a move event, which would leave the highlight stuck.
  public void updateHover(int screenX, int screenY) {
    scratch.set(screenX, screenY);
    context.getViewport().unproject(scratch);
    pointerWorldX = scratch.x;
    pointerWorldY = scratch.y;
    hoverRow = geometry.yToRow(scratch.y);
    hoverColumn = geometry.xToColumn(scratch.x);
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    updateHover(screenX, screenY);
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    updateHover(screenX, screenY);
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    if (paused) {
      return false;
    }
    updateHover(screenX, screenY);
    if (!isHoveringLawn()) {
      return false;
    }

    int row = hoverRow;
    int column = hoverColumn;

    // a sun on the tile always wins, it is what the player meant
    if (actions.collectSunAt(row, column)) {
      return true;
    }
    switch (tool) {
      case SEED:
        if (actions.plantAt(row, column, selectedPlantType)) {
          // packet is on cooldown now, staying armed would only produce refusals
          clearTool();
        }
        return true;
      case SHOVEL:
        if (actions.pluckAt(row, column)) {
          clearTool();
        }
        return true;
      case PLANT_FOOD:
        if (actions.feedPlantAt(row, column)) {
          clearTool();
        }
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode == Input.Keys.ESCAPE) {
      // with the pause menu up, escape closes it instead of leaving
      if (paused) {
        onTogglePause.run();
      } else {
        actions.requestExit();
      }
      return true;
    }
    if (keycode == Input.Keys.SPACE) {
      onTogglePause.run();
      return true;
    }
    if (paused) {
      return false;
    }
    int slot = seedSlotFor(keycode);
    if (slot >= 0) {
      if (slot < seedOrder.size()) {
        setSelectedPlantType(seedOrder.get(slot));
      }
      return true;
    }
    return false;
  }

  /** 0-based slot for a number key, or -1. Numpad too. */
  private static int seedSlotFor(int keycode) {
    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
      return keycode - Input.Keys.NUM_1;
    }
    if (keycode >= Input.Keys.NUMPAD_1 && keycode <= Input.Keys.NUMPAD_9) {
      return keycode - Input.Keys.NUMPAD_1;
    }
    return -1;
  }
}
