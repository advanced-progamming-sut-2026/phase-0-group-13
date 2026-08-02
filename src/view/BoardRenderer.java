package view;

import model.core.GameManager;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.minigame.DeadLineRule;
import model.game.minigame.LoveYourPlantsRule;
import model.game.minigame.TimedWarRule;
import model.game.minigame.arcade.BeghouledEngine;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.VasebreakerEngine;
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
    printSpecialStageHud(gm);

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

  private static void printSpecialStageHud(GameManager gm) {
    if (gm.getSpecialStageRule() instanceof TimedWarRule war) {
      System.out.printf("  TIME LEFT: %.1fs%n", war.remainingTicks() / 10.0);
    }
    if (gm.getSpecialStageRule() instanceof DeadLineRule deadLine) {
      System.out.printf("  DEAD LINE: column %d - a zombie reaching it loses the level%n",
              deadLine.getDeadlineColumn() + 1);
    }
    if (gm.getSpecialStageRule() instanceof LoveYourPlantsRule love) {
      System.out.printf("  Plants lost: %d/%d%n",
              gm.getMatchContext().getPlantsLost(), love.getLossBudget());
    }
  }
  private static void renderArcadeGrid(int rows, int cols, String header, String legend,
          java.util.function.IntFunction<String> rowLabel,
          java.util.function.BiFunction<Integer, Integer, String[]> cell) {
    String separator = buildSeparator(cols);
    System.out.println();
    System.out.println(separator);
    System.out.println(header);
    System.out.println(separator);
    System.out.println(buildColumnHeader(cols));
    System.out.println(buildGridLine(cols));
    String cellFormat = " %-" + CELL_TEXT_WIDTH + "s |";
    for (int row = 0; row < rows; row++) {
      StringBuilder entityLine = new StringBuilder(rowLabel.apply(row));
      StringBuilder healthLine = new StringBuilder("    |");
      for (int col = 0; col < cols; col++) {
        String[] cellData = cell.apply(row, col);
        entityLine.append(String.format(cellFormat, cellData[0]));
        healthLine.append(String.format(cellFormat, cellData[1]));
      }
      System.out.println(entityLine);
      System.out.println(healthLine);
      System.out.println(buildGridLine(cols));
    }
    System.out.println(legend);
  }
  public static void render(VasebreakerEngine engine) {
    renderArcadeGrid(VasebreakerEngine.ROWS, VasebreakerEngine.COLS,
            String.format("  Zombies: %d   |   Fresh seeds: %s",
                    engine.getZombies().size(), engine.getPendingSeedNames()),
            "Legend: V?=vase (?=unknown, G=green/plant, X=giant)   Z?=zombie   P?=plant"
                    + "   .=smashed   ZZ/V?=zombie standing on an unbroken vase",
            row -> String.format(" %-3d|", row + 1),
            (row, col) -> getVaseCellDetails(engine, row, col));
  }
  public static void render(IZombieEngine engine) {
    renderArcadeGrid(IZombieEngine.ROWS, IZombieEngine.COLS,
            String.format("  Zombie-Sun: %d   |   Brains left: %d/%d   |   deploy on the RIGHT,"
                            + " walk LEFT into the brains",
                    engine.getZombieSun(), engine.getBrainsRemaining(), IZombieEngine.BRAINS),
            "Legend: B=brain alive (row label)   x=brain eaten   PP=cutout plant   ZZ=your zombie\n"
                    + "Available: " + costList(engine),
            row -> String.format("%c%-3d|", engine.isBrainAlive(row) ? 'B' : 'x', row + 1),
            (row, col) -> getIZombieCellDetails(engine, row, col));
  }
  public static void render(BeghouledEngine engine) {
    renderArcadeGrid(BeghouledEngine.ROWS, BeghouledEngine.COLS,
            String.format("  Sun: %d   |   Matches: %d/%d",
                    engine.getSun(), engine.getMatchesMade(), engine.getMatchTarget()),
            "Legend: ZZ=zombie   ##=crater (nothing can go here)\n"
                    + plantGlyphLegend()
                    + "Upgrades: " + upgradeList(),
            row -> String.format(" %-3d|", row + 1),
            (row, col) -> getBeghouledCellDetails(engine, row, col));
  }
  private static String costList(IZombieEngine engine) {
    StringBuilder list = new StringBuilder();
    for (IZombieEngine.ZombieSpec spec : engine.availableZombieTypes()) {
      list.append(spec.name).append('(').append(spec.cost).append(")  ");
    }
    return list.toString();
  }
  private static String plantGlyphLegend() {
    StringBuilder legend = new StringBuilder("Plants: ");
    int printed = 0;
    for (BeghouledEngine.PlantKind kind : BeghouledEngine.PlantKind.values()) {
      if (printed > 0 && printed % 4 == 0) {
        legend.append("\n        ");
      }legend.append(String.format("%c%c=%-17s", kind.glyph, kind.glyph, kind.label));printed++;}
    return legend.append('\n').toString();}
  private static String upgradeList() {
    StringBuilder list = new StringBuilder();
    for (BeghouledEngine.Upgrade upgrade : BeghouledEngine.getUpgrades()) {
      list.append(upgrade.from.label).append("->").append(upgrade.to.label)
              .append('(').append(upgrade.cost).append(")  ");}
    return list.toString();}
  private static String[] getIZombieCellDetails(IZombieEngine engine, int row, int col) {
    StringBuilder entities = new StringBuilder();
    StringBuilder healths = new StringBuilder();

    int plantHealth = engine.getPlantHealthAt(row, col);
    if (plantHealth >= 0) {
      entities.append("PP");
      healths.append(plantHealth);
    }
    int zombieHealth = engine.getZombieHealthAt(row, col);
    if (zombieHealth >= 0) {
      if (entities.length() > 0) {entities.append("/");healths.append("/");}
      entities.append("ZZ");healths.append(zombieHealth);
    }
    return new String[] {entities.length() == 0 ? "." : entities.toString(),
            healths.length() == 0 ? "-" : healths.toString()};
  }
  private static String[] getBeghouledCellDetails(BeghouledEngine engine, int row, int col) {
    StringBuilder entities = new StringBuilder();
    StringBuilder healths = new StringBuilder();
    BeghouledEngine.PlantKind kind = engine.getPlantAt(row, col);
    if (engine.isCraterAt(row, col)) {entities.append("##");
    } else if (kind != null) {
      entities.append(kind.glyph).append(kind.glyph);}
    int zombieHealth = engine.getZombieHealthAt(row, col);
    if (zombieHealth >= 0) {
      if (entities.length() > 0) {
        entities.append("/");
      }entities.append("ZZ");healths.append(zombieHealth);
    }
    return new String[] {entities.length() == 0 ? "." : entities.toString(),
            healths.length() == 0 ? "-" : healths.toString()};
  }
  private static String[] getVaseCellDetails(VasebreakerEngine engine, int row, int col) {
    StringBuilder entities = new StringBuilder();
    StringBuilder healths = new StringBuilder();
    for (VasebreakerEngine.ArcadeZombie zombie : engine.getZombies()) {
      if (zombie.getRow() == row && zombie.getColumn() == col && !zombie.isDead()) {
        entities.append('Z').append(zombie.isGargantuar() ? 'G' : 'Z');
        healths.append(zombie.getHealth());
        break;
      }
    }
    int plantHealth = engine.getPlantHealthAt(row, col);
    if (plantHealth >= 0) {
      if (entities.length() > 0) {
        entities.append("/");
        healths.append("/");
      }entities.append("PP");healths.append(plantHealth);}
    if (!engine.getSmashedGrid()[row][col]) {
      if (entities.length() > 0) {
        entities.append("/");healths.append("/");}
      entities.append('V').append(switch (engine.getVaseGrid()[row][col]) {
        case PLANT_VASE -> 'G';
        case GARGANTUAR_VASE -> 'X';
        default -> '?';
      });
      healths.append('-');
    }
    return new String[] {entities.length() == 0 ? "." : entities.toString(),
            healths.length() == 0 ? "-" : healths.toString()};
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
      Plant top = board.getTopPlantAt(row, col);
      Plant extra = top != plant ? top : plant.getShield();
      if (extra != null) {
        entities.append("/P").append(initialOf(extra.getName()));
        healths.append("/").append(extra.getCurrentHealth());
      }
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
