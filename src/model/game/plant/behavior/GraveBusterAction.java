package model.game.plant.behavior;

import model.game.Board;
import model.game.TileEffects.TombStoneEffect;
import model.game.Tile;
import model.game.plant.Plant;

public class GraveBusterAction implements PlantAction {

  private static final int DEFAULT_EAT_TICKS = 30;

  private final int eatTicks;
  private boolean started;

  public GraveBusterAction(int eatTicks) {
    this.eatTicks = Math.max(0, eatTicks);
  }

  public GraveBusterAction() {
    this(DEFAULT_EAT_TICKS);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    Tile tile = board.getTile(plant.getRow(), plant.getCol());
    TombStoneEffect grave = tile != null && tile.getEffect() instanceof TombStoneEffect stone
            ? stone : null;

    if (grave == null || !grave.isActive()) {
      System.out.printf("%s found no gravestone here and was wasted.%n", plant.getName());
      plant.takeDamage(plant.getMaxHealth());
      return;
    }

    if (!started) {
      started = true;
      plant.setLastActionTick(currentTick);
      return;
    }
    if (currentTick - plant.getLastActionTick() < eatTicks) {
      return;
    }

    grave.breakStone();
    tile.setEffect(null);
    System.out.printf("%s destroyed the gravestone at (%d, %d).%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    plant.takeDamage(plant.getMaxHealth());
  }
}
