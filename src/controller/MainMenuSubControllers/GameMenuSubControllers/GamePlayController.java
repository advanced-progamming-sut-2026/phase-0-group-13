package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import java.util.regex.Matcher;
import model.account.AdventureMap;
import model.account.Progress;
import model.account.User;
import model.core.App;
import model.core.GameManager;
import model.core.GameSession;
import model.enums.Commands.GamePlayMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;
import model.game.Board;
import model.game.MatchResult;
import model.game.Sun;
import model.game.Tile;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

public class GamePlayController implements BaseController {
  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    GameManager gm = GameSession.getActiveGame();
    if (gm == null) {
      System.out.println("error: no active game. Return to the Game Menu.");
      App.setCurrentMenu(Menu.GameMenu);
      return;
    }

    if (!dispatch(command, gm)) {
      System.out.println("invalid input");
    }
  }

  /** Returns true if the command matched something. */
  private boolean dispatch(String command, GameManager gm) {
    Matcher m;
    if ((m = GamePlayMenuCommands.AdvanceTime.getMatcher(command)) != null) {
      handleAdvance(gm, m);
    } else if ((m = GamePlayMenuCommands.PlantPlant.getMatcher(command)) != null) {
      handlePlant(gm, m);
    } else if ((m = GamePlayMenuCommands.PluckPlant.getMatcher(command)) != null) {
      handlePluck(gm, m);
    } else if ((m = GamePlayMenuCommands.CollectSun.getMatcher(command)) != null) {
      handleCollectSun(gm, m);
    } else if ((m = GamePlayMenuCommands.FeedPlant.getMatcher(command)) != null) {
      handleFeed(gm, m);
    } else if ((m = GamePlayMenuCommands.ShowTileStatus.getMatcher(command)) != null) {
      handleTileStatus(gm, m);
    } else if ((m = GamePlayMenuCommands.CheatAddSuns.getMatcher(command)) != null) {
      handleCheatAddSuns(gm, m);
    } else if ((m = GamePlayMenuCommands.CheatSpawnZombie.getMatcher(command)) != null) {
      handleSpawnZombie(gm, m);
    } else {
      return dispatchNoArg(command, gm);
    }
    return true;
  }

  /** Second half of dispatch for the argument-less commands (keeps each method short). */
  private boolean dispatchNoArg(String command, GameManager gm) {
    if (GamePlayMenuCommands.ShowMap.getMatcher(command) != null) {
      renderMap(gm);
    } else if (GamePlayMenuCommands.ShowSunAmount.getMatcher(command) != null) {
      System.out.println("Sun: " + gm.getSunAmount());
    } else if (GamePlayMenuCommands.ShowPlantsStatus.getMatcher(command) != null) {
      handlePlantsStatus(gm);
    } else if (GamePlayMenuCommands.ZombiesInfo.getMatcher(command) != null) {
      handleZombiesInfo(gm);
    } else if (GamePlayMenuCommands.CheatAddPlantFood.getMatcher(command) != null) {
      gm.getBoard().getGameState().addPlantFood();
    } else if (GamePlayMenuCommands.CheatRemoveCooldown.getMatcher(command) != null) {
      System.out.println("All plant cooldowns cleared.");
    } else if (GamePlayMenuCommands.ReleaseTheNuke.getMatcher(command) != null) {
      handleNuke(gm);
    } else if (GamePlayMenuCommands.StartZombieWaves.getMatcher(command) != null) {
      System.out.println("Zombie waves started.");
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("GamePlay Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      return false;
    }
    return true;
  }

  // ---------------------------------------------------------------- command handlers

  private void handleAdvance(GameManager gm, Matcher m) {
    int count = parseInt(m.group("count"));
    if (count <= 0) {
      System.out.println("error: tick count must be a positive number");
      return;
    }
    for (int i = 0; i < count && gm.isRunning(); i++) {
      gm.advanceTime();
    }
    if (!gm.isRunning()) {
      finishMatch(gm);
    }
  }

  private void handlePlant(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    if (GameDataManager.plantRepository == null) {
      System.out.println("error: plant data is not loaded");
      return;
    }
    String type = m.group("type").trim();
    Plant plant = null;
    try {
      plant = new PlantFactory(GameDataManager.plantRepository).createPlant(type, rc[0], rc[1]);
    } catch (RuntimeException e) {
      System.out.println("error: could not build plant '" + type + "'");
      return;
    }
    if (plant == null) {
      System.out.println("error: unknown plant '" + type + "'");
      return;
    }
    if (gm.placePlant(plant, rc[0], rc[1])) {
      System.out.printf("Planted %s at (%s, %s).%n", type, m.group("x"), m.group("y"));
    } else {
      System.out.println("error: cannot plant there (tile occupied or not enough sun)");
    }
  }

  private void handlePluck(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Plant plant = gm.getBoard().getPlantAt(rc[0], rc[1]);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    gm.getBoard().getPlants().remove(plant);
    System.out.printf("Plucked %s.%n", plant.getName());
  }

  private void handleCollectSun(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    for (Sun sun : gm.getBoard().getSuns()) {
      if (!sun.isExpired()
          && Math.round(sun.getX()) == rc[1]
          && Math.round(sun.getY()) == rc[0]) {
        gm.collectSun(sun);
        return;
      }
    }
    System.out.println("error: no sun to collect at that tile");
  }

  private void handleFeed(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Plant plant = gm.getBoard().getPlantAt(rc[0], rc[1]);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    gm.usePlantFood(plant);
  }

  private void handleCheatAddSuns(GameManager gm, Matcher m) {
    int count = parseInt(m.group("count"));
    if (count <= 0) {
      System.out.println("error: count must be a positive number");
      return;
    }
    gm.getBoard().getGameState().addSun(count);
  }

  private void handleSpawnZombie(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    if (GameDataManager.zombieRepository == null) {
      System.out.println("error: zombie data is not loaded");
      return;
    }
    String type = m.group("zombieType").trim();
    try {
      Zombie zombie =
          new ZombieFactory(GameDataManager.zombieRepository).createZombie(type, rc[0], rc[1]);
      if (zombie == null) {
        System.out.println("error: unknown zombie type '" + type + "'");
        return;
      }
      gm.getBoard().spawnZombie(zombie);
      System.out.println("Spawned zombie " + type + ".");
    } catch (RuntimeException e) {
      System.out.println("error: could not spawn zombie (data unavailable)");
    }
  }

  private void handleNuke(GameManager gm) {
    int killed = 0;
    for (Zombie z : gm.getBoard().getZombies()) {
      if (!z.isDead()) {
        z.takeDamage(1_000_000, true);
        killed++;
      }
    }
    System.out.println("Nuke released. Zombies wiped: " + killed);
  }

  private void handlePlantsStatus(GameManager gm) {
    List<Plant> plants = gm.getBoard().getPlants();
    if (plants.isEmpty()) {
      System.out.println("No plants on the field.");
      return;
    }
    for (Plant p : plants) {
      System.out.printf(
          "  %s at (%d, %d) - hp %d%n", p.getName(), p.getCol() + 1, p.getRow() + 1,
          p.getCurrentHealth());
    }
  }

  private void handleTileStatus(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Board board = gm.getBoard();
    Plant plant = board.getPlantAt(rc[0], rc[1]);
    Tile tile = board.getTile(rc[0], rc[1]);
    System.out.printf("Tile (%s, %s):%n", m.group("x"), m.group("y"));
    System.out.println(
        "  plant: " + (plant == null ? "none" : plant.getName() + " (hp " + plant.getCurrentHealth() + ")"));
    StringBuilder zs = new StringBuilder();
    for (Zombie z : board.getZombies()) {
      if (Math.round(z.getX()) == rc[1] && z.getRow() == rc[0]) {
        zs.append(z.getName()).append(' ');
      }
    }
    System.out.println("  zombies: " + (zs.length() == 0 ? "none" : zs.toString().trim()));
    System.out.println(
        "  effect: "
            + (tile == null || tile.getEffect() == null
                ? "none"
                : tile.getEffect().getClass().getSimpleName()));
  }

  private void handleZombiesInfo(GameManager gm) {
    List<Zombie> zombies = gm.getBoard().getZombies();
    if (zombies.isEmpty()) {
      System.out.println("No zombies on the field.");
      return;
    }
    for (Zombie z : zombies) {
      System.out.println(z.getName() + ":");
      System.out.printf("  position: %.1f, %d%n", z.getX() + 1, z.getRow() + 1);
      System.out.println("  health: " + z.getCurrentHealth());
    }
  }

  // ---------------------------------------------------------------- rendering

  private void renderMap(GameManager gm) {
    Board board = gm.getBoard();
    System.out.printf(
        "=== Wave %d/%d | Sun: %d | Plant Food: %d ===%n",
        gm.getCurrentWaveIndex() + 1, gm.getTotalWaves(), gm.getSunAmount(),
        gm.getPlantFoodCount());
    for (int row = 0; row < board.getRows(); row++) {
      StringBuilder line = new StringBuilder();
      for (int col = 0; col < board.getColumns(); col++) {
        line.append('[').append(cellGlyph(board, row, col)).append(']');
      }
      String mower = board.getLawnmowers().get(row).isActive() ? "mower" : "used";
      System.out.println(line + "  " + mower);
    }
  }

  private char cellGlyph(Board board, int row, int col) {
    for (Zombie z : board.getZombies()) {
      if (z.getRow() == row && Math.round(z.getX()) == col) {
        return 'Z';
      }
    }
    Plant plant = board.getPlantAt(row, col);
    if (plant != null && plant.getName() != null && !plant.getName().isEmpty()) {
      return Character.toUpperCase(plant.getName().charAt(0));
    }
    Tile tile = board.getTile(row, col);
    if (tile != null && tile.getEffect() != null) {
      return '#';
    }
    return '.';
  }

  // ---------------------------------------------------------------- helpers

  private void finishMatch(GameManager gm) {
    MatchResult result = gm.getMatchResult();
    if (result.isWon()) {
      System.out.println(
          "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    } else {
      System.out.println("You lost the battle.");
    }

    applyProgression(gm, result);

    Menu back = GameSession.getReturnMenu();
    GameSession.end();
    App.setCurrentMenu(back);
  }

  /** Applies match outcome to the logged-in user: record, MyoPoints, rewards, adventure step. */
  private void applyProgression(GameManager gm, MatchResult result) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }

    user.addMatchResult(result);
    gm.getScoreManager().applyScoresToUser(user);

    if (result.isWon()) {
      Progress progress = user.getProgress();
      model.Result reward =
          AdventureMap.getLevelReward(progress.getCurrentStage(), progress.getCurrentLevel());
      if (reward.success() && reward.getObject() instanceof String unlockId) {
        System.out.println(reward.message());
        if (!unlockId.contains("trophy")) {
          user.unlockPlant(unlockId);
        }
      }
      System.out.println(progress.advanceAdventure().message());
    }

    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  /** Converts 1-indexed command coords (x=col, y=row) to 0-indexed {row, col}; null if invalid. */
  private int[] parseCoord(Board board, String xStr, String yStr) {
    int x = parseInt(xStr);
    int y = parseInt(yStr);
    if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
      System.out.println("error: coordinates must be numbers");
      return null;
    }
    int col = x - 1;
    int row = y - 1;
    if (board == null
        || row < 0
        || row >= board.getRows()
        || col < 0
        || col >= board.getColumns()) {
      System.out.println("error: coordinates out of bounds");
      return null;
    }
    return new int[] {row, col};
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return Integer.MIN_VALUE;
    }
  }

  @Override
  public void exit() {
    Menu back = GameSession.getReturnMenu();
    GameSession.end();
    System.out.println("Left the battle. Changed to Game Menu.");
    App.setCurrentMenu(back);
  }
}
