package view;

import model.core.GameManager;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public final class BoardRenderer {

  private static final int CELL_TEXT_WIDTH = 6;
  private static final String ROW_PREFIX_PAD = "     ";
  private static final String ZOMBIE_PREFIX = "Zombie";

  private BoardRenderer() {}

  public static void render(GameManager gm) {
    Board board = gm.getBoard();
    String separator = buildSeparator(board.getColumns());

    System.out.println();
    System.out.println(separator);
    System.out.printf(
            "  Wave: %d/%d   |   Sun: %d   |   Plant Food: %d/%d   |   Time: %.1fs%n",
            Math.min(gm.getCurrentWaveIndex() + 1, Math.max(gm.getTotalWaves(), 1)),
            gm.getTotalWaves(),
            gm.getSunAmount(),
            gm.getPlantFoodCount(),
            GameState.MAX_PLANT_FOOD,
            gm.getCurrentTick() / 10.0);
    System.out.println(separator);

    System.out.println(buildColumnHeader(board.getColumns()));
    System.out.println(buildGridLine(board.getColumns()));

    String cellFormat = " %-" + CELL_TEXT_WIDTH + "s |";
    for (int row = 0; row < board.getRows(); row++) {
      printRow(board, row, cellFormat);
      System.out.println(buildGridLine(board.getColumns()));
    }

    System.out.println(
            "Legend: Z?=zombie   P?=plant   ~=water   +=gravestone   .=empty tile"
                    + "   (? = first letter of the name)");
    System.out.println(
            "        Second line of every row is the current HP."
                    + " Coordinates are (column, row), both 1-indexed.");
  }

  private static void printRow(Board board, int row, String cellFormat) {
    StringBuilder entityLine = new StringBuilder(String.format(" %-3d|", row + 1));
    StringBuilder healthLine = new StringBuilder("    |");

    for (int col = 0; col < board.getColumns(); col++) {
      String[] cellData = getCellDetails(board, row, col);
      entityLine.append(String.format(cellFormat, cellData[0]));
      healthLine.append(String.format(cellFormat, cellData[1]));
    }

    String mower = board.getLawnmowers().get(row).isActive() ? "  [MOWER]" : "  [ used]";
    System.out.println(entityLine + mower);
    System.out.println(healthLine);
  }

  private static String buildSeparator(int columns) {
    return "=".repeat(ROW_PREFIX_PAD.length() + columns * (CELL_TEXT_WIDTH + 3));
  }

  private static String buildColumnHeader(int columns) {
    StringBuilder header = new StringBuilder(ROW_PREFIX_PAD + " ");
    for (int col = 1; col <= columns; col++) {
      header.append(String.format("%-" + (CELL_TEXT_WIDTH + 3) + "s", col));
    }
    return header.toString();
  }

  private static String buildGridLine(int columns) {
    StringBuilder line = new StringBuilder("    +");
    for (int col = 0; col < columns; col++) {
      line.append("-".repeat(CELL_TEXT_WIDTH + 2)).append("+");
    }
    return line.toString();
  }

  private static String[] getCellDetails(Board board, int row, int col) {
    StringBuilder entities = new StringBuilder();
    StringBuilder healths = new StringBuilder();

    for (Zombie z : board.getZombies()) {
      if (z.getRow() == row && Math.round(z.getX()) == col && !z.isDead()) {
        entities.append("Z").append(initialOf(z.getName()));
        healths.append(z.getCurrentHealth());
        break;
      }
    }

    Plant plant = board.getPlantAt(row, col);
    if (plant != null && plant.getName() != null && !plant.getName().isEmpty()) {
      if (entities.length() > 0) {
        entities.append("/");
        healths.append("/");
      }
      entities.append("P").append(initialOf(plant.getName()));
      healths.append(plant.getCurrentHealth());
    }

    Tile tile = board.getTile(row, col);
    if (entities.length() == 0 && tile != null) {
      if (tile.isWater()) {
        entities.append("~");
      } else if (tile.getEffect() != null) {
        entities.append("+");
      }
    }

    if (entities.length() == 0) {
      entities.append(".");
    }
    if (healths.length() == 0) {
      healths.append("-");
    }

    return new String[] {entities.toString(), healths.toString()};
  }

  private static char initialOf(String name) {
    if (name == null || name.isEmpty()) {
      return '?';
    }
    String trimmed = name;
    if (trimmed.regionMatches(true, 0, ZOMBIE_PREFIX, 0, ZOMBIE_PREFIX.length())
            && trimmed.length() > ZOMBIE_PREFIX.length()) {
      trimmed = trimmed.substring(ZOMBIE_PREFIX.length());
    }
    return trimmed.isEmpty() ? '?' : Character.toUpperCase(trimmed.charAt(0));
  }
}
