package view.gdx.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import view.gdx.audio.GameAudio;
import view.gdx.render.LawnGeometry;
import view.gdx.render.RenderContext;


public final class GameplayInputHandler extends InputAdapter {

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

  private final Vector2 scratch = new Vector2();

  private final List<String> seedOrder = new ArrayList<>();

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

  public void updateHover(int screenX, int screenY) {
    scratch.set(screenX, screenY);
    context.getViewport().unproject(scratch);
    pointerWorldX = scratch.x;
    pointerWorldY = scratch.y;
    hoverRow = geometry.yToRow(scratch.y);
    hoverColumn = geometry.xToColumn(scratch.x);
  }

  public void collectSunUnderPointer() {
    if (paused || !isHoveringLawn()) {
      return;
    }
    if (actions.collectSunByHover(hoverRow, hoverColumn)) {
      GameAudio.getInstance().play(GameAudio.Sfx.SUN);
    }
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

    if (actions.collectSunAt(row, column)) {
      GameAudio.getInstance().play(GameAudio.Sfx.SUN);
      return true;
    }
    // Before the tool, so picking a dose up off the lawn never plants on top of it by mistake.
    if (actions.collectPlantFoodAt(row, column)) {
      GameAudio.getInstance().play(GameAudio.Sfx.SUN);
      return true;
    }
    switch (tool) {
      case SEED:
        if (actions.plantAt(row, column, selectedPlantType)) {
          GameAudio.getInstance().play(GameAudio.Sfx.PLANT);
          clearTool();
        }
        return true;
      case SHOVEL:
        if (actions.pluckAt(row, column)) {
          GameAudio.getInstance().play(GameAudio.Sfx.PLANT);
          clearTool();
        }
        return true;
      case PLANT_FOOD:
        if (actions.feedPlantAt(row, column)) {
          GameAudio.getInstance().play(GameAudio.Sfx.EXPLODE);
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
      if (paused) {
        onTogglePause.run();
      } else if (tool != Tool.NONE) {
        clearTool();
      } else {
        onTogglePause.run();
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
