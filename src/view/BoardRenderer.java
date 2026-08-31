package view;

import model.core.GameManager;
import model.enums.StatusEffect;
import model.game.Board;
import model.game.GameState;
import model.game.Tile;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.minigame.DeadLineRule;
import model.game.minigame.LoveYourPlantsRule;
import model.game.minigame.TimedWarRule;
import model.game.minigame.arcade.BeghouledEngine;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.VasebreakerEngine;
import model.game.minigame.arcade.WallnutBowlingEngine;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTypeResolver;

public final class BoardRenderer {

  private static final int CELL_TEXT_WIDTH = 8;
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
            "Legend: Z?=zombie   P?=plant   ~=water   +=grave (+$=50 sun, +F=plant food)"
                    + "   *=frozen ground   ^/v=slider ice   !!=burning   #=barrel   .=empty tile"
                    + "   (? = first letter of the name)");
    System.out.println(
            "        Second line of every row is the current HP (zombies show body+armour)."
                    + " Coordinates are (column, row), both 1-indexed.");
  }

  public static void renderDebug(GameManager gm) {
    render(gm);
    printZombieDebugTable(gm.getBoard(), gm.getCurrentTick());
    printHazardDebugTable(gm.getBoard());
  }

  private static void printZombieDebugTable(Board board, int currentTick) {
    System.out.println();
    System.out.println("[debug] zombies (body hp | armour hp | damage taken body/armour):");
    boolean any = false;
    for (Zombie z : board.getZombies()) {
      if (z.isDead()) {
        continue;
      }
      if (!any) {
        System.out.printf("  %-34s %-10s %-11s %-14s %-10s %s%n",
                "name (type)", "position", "body hp", "armour hp", "dmg b/a", "state");
        any = true;
      }
      String ability = z.getBehavior() == null ? null : z.getBehavior().debugState(z, currentTick);
      System.out.printf("  %-34s %-10s %-11s %-14s %-10s %s%n",
              fit(z.getDisplayName(), 20) + " (" + fit(typeOf(z), 10) + ")",
              String.format("%.1f,%d", z.getX() + 1, z.getRow() + 1),
              z.getCurrentHealth() + "/" + z.getMaxHealth(),
              armourOf(z),
              z.getBodyDamageTaken() + "/" + z.getArmorDamageTaken(),
              stateOf(z) + (ability == null ? "" : " | " + ability));
    }
    if (!any) {
      System.out.println("  none");
    }
  }

  private static void printHazardDebugTable(Board board) {
    System.out.println("[debug] tile hazards:");
    boolean any = false;
    for (int row = 0; row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        Tile tile = board.getTile(row, col);
        if (tile == null || tile.getEffect() == null || !tile.getEffect().isActive()) {
          continue;
        }
        any = true;
        System.out.printf("  (%d, %d) %s%n", col + 1, row + 1, hazardDetails(tile.getEffect()));
      }
    }
    for (Plant plant : board.getPlants()) {
      if (plant.getIceHealth() > 0) {
        any = true;
        System.out.printf("  (%d, %d) Ice block on %s: %d/%d hp%n",
                plant.getCol() + 1, plant.getRow() + 1, plant.getName(),
                plant.getIceHealth(), Plant.ICE_BLOCK_HEALTH);
      }
    }
    if (!any) {
      System.out.println("  none");
    }
  }

  private static String hazardDetails(model.game.TileEffects.TileEffect effect) {
    if (effect instanceof TombStoneEffect grave) {
      return "Grave: " + grave.getHealth() + " hp"
              + (grave.isBlocksShots() ? ", blocks shots" : "")
              + (grave.getBuriedReward() == null ? "" : ", holds " + grave.getBuriedReward());
    }
    if (effect instanceof IceTrailEffect ice) {
      return ice.isFullFreeze()
              ? "Frozen ground (zombies freeze here)"
              : "Slider ice: pushes zombies " + (ice.getSlideDirection() < 0 ? "up" : "down");
    }
    if (effect instanceof model.game.TileEffects.FireEffect) {
      return "Burning ground (nothing can be planted while it burns)";
    }
    return effect.getName();
  }

  private static String armourOf(Zombie zombie) {
    if (zombie.getArmors().isEmpty()) {
      return "-";
    }
    return zombie.getRemainingArmorHealth() + "/" + zombie.getMaxArmorHealth()
            + (zombie.isArmorBroken() ? " BROKEN" : "");
  }

  public static String typeOf(Zombie zombie) {
    if (zombie == null || zombie.getName() == null || data.GameDataManager.zombieRepository == null) {
      return "UNKNOWN";
    }
    var template = data.GameDataManager.zombieRepository.find(zombie.getName());
    return template == null ? "UNKNOWN" : ZombieTypeResolver.resolve(template).name();
  }

  public static String stateOf(Zombie zombie) {
    StringBuilder state = new StringBuilder();
    Integer frozen = zombie.getActiveEffects().get(StatusEffect.FROZEN);
    Integer chilled = zombie.getActiveEffects().get(StatusEffect.CHILLED);
    if (frozen != null) {
      appendState(state, String.format("frozen %.1fs", frozen / 10.0));
    }
    if (chilled != null) {
      appendState(state, String.format("chilled %.1fs", chilled / 10.0));
    }
    if (zombie.isEating()) {
      appendState(state, "eating");
    } else if (frozen == null) {
      appendState(state, "walking");
    }
    if (zombie.isHypnotized()) {
      appendState(state, "hypnotized");
    }
    if (zombie.isSubmerged()) {
      appendState(state, "submerged");
    }
    if (zombie.hasShieldBlocker()) {
      appendState(state, "lob-proof");
    }
    if (zombie.isShiny()) {
      appendState(state, "glowing");
    }
    if (zombie.isArmorBroken()) {
      appendState(state, "armour broken");
    }
    return state.length() == 0 ? "idle" : state.toString();
  }

  private static void appendState(StringBuilder state, String text) {
    if (state.length() > 0) {
      state.append(", ");
    }
    state.append(text);
  }

  private static String fit(String text, int width) {
    if (text == null) {
      return "";
    }
    return text.length() <= width ? text : text.substring(0, width);
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
    printBossHud(gm);
  }

  /** داک: در مرحلهٔ باس، به‌جای نوار موج، نوار جانِ سه‌تکه نشان داده می‌شود. */
  private static void printBossHud(GameManager gm) {
    if (!(gm.getSpecialStageRule() instanceof model.game.minigame.BossStageRule boss)) {
      return;
    }
    Zombie zomboss = boss.getBoss();
    if (zomboss == null) {
      System.out.println("  BOSS STAGE: Dr. Zomboss has not arrived yet.");
      return;
    }
    model.game.zombie.behavior.ZombossHealth health = boss.getBossHealth();
    StringBuilder bar = new StringBuilder();
    for (int i = model.game.zombie.behavior.ZombossHealth.SEGMENTS - 1; i >= 0; i--) {
      bar.append('[').append(segmentBar(health, i, zomboss.getCurrentHealth())).append(']');
    }
    System.out.printf("  DR. ZOMBOSS: %s  rows %d-%d%s%n", bar,
            zomboss.getRow() + 1, zomboss.getBottomRow() + 1,
            boss.isBossStunned() ? "   *** STUNNED ***" : "");
  }

  private static String segmentBar(
          model.game.zombie.behavior.ZombossHealth health, int segment, int currentHealth) {
    if (health == null) {
      return "?????";
    }
    int filled = Math.round(health.fractionOf(segment, currentHealth) * BOSS_SEGMENT_CELLS);
    return "#".repeat(filled) + "-".repeat(BOSS_SEGMENT_CELLS - filled);
  }

  private static final int BOSS_SEGMENT_CELLS = 8;
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
        entityLine.append(String.format(cellFormat, fit(cellData[0])));
        healthLine.append(String.format(cellFormat, fit(cellData[1])));
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
            "Legend: V?=vase (?=unknown, G=green/plant, X=giant)   xx=opened vase   Z?=zombie"
                    + "   P?=plant   SS=seed packet on the ground   .=empty tile",
            row -> String.format(" %-3d|", row + 1),
            (row, col) -> getVaseCellDetails(engine, row, col));
  }
  public static void render(WallnutBowlingEngine engine) {
    renderArcadeGrid(WallnutBowlingEngine.LANES, WallnutBowlingEngine.LANE_LENGTH,
            String.format("  Level: %d   |   Score: %d   |   Zombies left to spawn: %d"
                            + "   |   On the belt: %s",
                    engine.getLevel(), engine.getScore(), engine.getZombiesRemainingToSpawn(),
                    engine.getReadyNutLabel()),
            "Legend: ZZ=zombie   OO=bowling wall-nut   EE=explode-o-nut   GG=giant wall-nut\n"
                    + "        ||=red line - you may only plant in columns 1-"
                    + (WallnutBowlingEngine.RED_LINE_COLUMN + 1),
            row -> String.format(" %-3d|", row + 1),
            (row, col) -> getBowlingCellDetails(engine, row, col));
  }
  public static void render(IZombieEngine engine) {
    renderArcadeGrid(IZombieEngine.ROWS, IZombieEngine.COLS,
            String.format("  Zombie-Sun: %d   |   Brains left: %d/%d   |   deploy on the RIGHT,"
                            + " walk LEFT into the brains",
                    engine.getZombieSun(), engine.getBrainsRemaining(), IZombieEngine.BRAINS),
            "Legend: B=brain alive (row label)   x=brain eaten   PP=cutout plant   ZZ=your zombie\n"
                    + "        ||=red line - you may only deploy in columns "
                    + (IZombieEngine.RED_LINE_COLUMN + 2) + "-" + IZombieEngine.COLS + "\n"
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
                    + "Upgrades: " + upgradeList(engine),
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
  private static String upgradeList(BeghouledEngine engine) {
    StringBuilder list = new StringBuilder();
    for (BeghouledEngine.Upgrade upgrade : engine.getUpgrades()) {
      list.append(upgrade.from.label).append("->").append(upgrade.to.label)
              .append('(').append(upgrade.cost).append(")  ");}
    return list.toString();}
  public static String[] getBowlingCellDetails(WallnutBowlingEngine engine, int row, int col) {
    StringBuilder entities = new StringBuilder();
    StringBuilder healths = new StringBuilder();

    WallnutBowlingEngine.NutType nut = engine.getNutTypeAt(row, col);
    if (nut != null) {
      entities.append(nut.glyph).append(nut.glyph);
      healths.append('-');
    }
    int zombieHealth = engine.getZombieHealthAt(row, col);
    if (zombieHealth >= 0) {
      if (entities.length() > 0) {entities.append("/");healths.append("/");}
      entities.append("ZZ");healths.append(zombieHealth);
    }
    if (col == WallnutBowlingEngine.RED_LINE_COLUMN) {
      if (entities.length() > 0) {entities.append("/");healths.append("/");}
      entities.append("||");healths.append('-');
    }
    return new String[] {entities.length() == 0 ? "." : entities.toString(),
            healths.length() == 0 ? "-" : healths.toString()};
  }
  public static String[] getIZombieCellDetails(IZombieEngine engine, int row, int col) {
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
    if (col == IZombieEngine.RED_LINE_COLUMN) {
      if (entities.length() > 0) {entities.append("/");healths.append("/");}
      entities.append("||");healths.append('-');
    }
    return new String[] {entities.length() == 0 ? "." : entities.toString(),
            healths.length() == 0 ? "-" : healths.toString()};
  }
  public static String[] getBeghouledCellDetails(BeghouledEngine engine, int row, int col) {
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
  public static String[] getVaseCellDetails(VasebreakerEngine engine, int row, int col) {
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
    if (engine.hasPendingSeedAt(row, col)) {
      if (entities.length() > 0) {
        entities.append("/");healths.append("/");}
      entities.append("SS");healths.append('-');
    }
    VasebreakerEngine.VaseContent vase = engine.getVaseGrid()[row][col];
    if (vase != VasebreakerEngine.VaseContent.NONE) {
      if (entities.length() > 0) {
        entities.append("/");healths.append("/");}
      if (engine.getSmashedGrid()[row][col]) {
        entities.append("xx");
      } else {
        entities.append('V').append(switch (vase) {
          case PLANT_VASE -> 'G';
          case GARGANTUAR_VASE -> 'X';
          default -> '?';
        });
      }
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
      entityLine.append(String.format(cellFormat, fit(cellData[0])));
      healthLine.append(String.format(cellFormat, fit(cellData[1])));
    }

    String mower = board.getLawnmowers().get(row).isActive() ? "  [MOWER]" : "  [ used]";
    System.out.println(entityLine + mower);
    System.out.println(healthLine);
  }

  private static String fit(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= CELL_TEXT_WIDTH ? text : text.substring(0, CELL_TEXT_WIDTH);
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
        entities.append("Z").append(initialOf(z.getDisplayName()));
        int armorLeft = z.getRemainingArmorHealth();
        healths.append(z.getCurrentHealth());
        if (armorLeft > 0) {
          healths.append("+").append(armorLeft);
        }
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
      if (tile.getEffect() != null && tile.getEffect().isActive()) {
        entities.append(glyphFor(tile.getEffect()));
      } else if (tile.isWater()) {
        entities.append("~");
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

  /** طبق داک، انواع زمین/هزارد باید روی نقشه از هم قابل تشخیص باشند. */
  private static String glyphFor(model.game.TileEffects.TileEffect effect) {
    if (effect instanceof TombStoneEffect grave) {
      String reward = grave.getBuriedReward();
      if (reward == null) {
        return "+";
      }
      return "SUN".equals(reward) ? "+$" : "+F";
    }
    if (effect instanceof IceTrailEffect ice) {
      if (ice.isFullFreeze()) {
        return "*";
      }
      return ice.getSlideDirection() < 0 ? "^" : "v";
    }
    if (effect instanceof model.game.TileEffects.FireEffect) {
      return "!!";
    }
    return "#";
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
