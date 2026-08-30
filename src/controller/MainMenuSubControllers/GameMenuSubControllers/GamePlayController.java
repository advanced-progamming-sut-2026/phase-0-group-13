package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import java.util.regex.Matcher;
import model.account.User;
import model.core.App;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchCompletion;
import model.core.MatchSetup;
import model.enums.Commands.GamePlayMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;
import model.game.Board;
import model.game.Tile;
import model.game.plant.Plant;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.quest.MatchContext;
import model.game.zombie.Zombie;

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
      GamePlayCheatHandler.addSuns(gm, parseInt(m.group("count")));
    } else if ((m = GamePlayMenuCommands.CheatSpawnZombie.getMatcher(command)) != null) {
      handleSpawnZombie(gm, m);
    } else {
      return dispatchNoArg(command, gm);
    }
    return true;
  }

  private boolean dispatchNoArg(String command, GameManager gm) {
    if (GamePlayMenuCommands.ShowMapDebug.getMatcher(command) != null) {
      view.BoardRenderer.renderDebug(gm);
    } else if (GamePlayMenuCommands.ShowMap.getMatcher(command) != null) {
      view.BoardRenderer.render(gm);
    } else if (GamePlayMenuCommands.ShowSunAmount.getMatcher(command) != null) {
      System.out.println("Sun: " + gm.getSunAmount());
    } else if (GamePlayMenuCommands.ShowPlantsStatus.getMatcher(command) != null) {
      handlePlantsStatus(gm);
    } else if (GamePlayMenuCommands.ZombiesInfo.getMatcher(command) != null) {
      handleZombiesInfo(gm);
    } else if (GamePlayMenuCommands.CheatAddPlantFood.getMatcher(command) != null) {
      if (gm.getBoard().getGameState().addPlantFood()) {
        System.out.println("1 Plant food added.");
      } else {
        System.out.println("error: you cannot hold more than 3 plant foods");
      }
    } else if (GamePlayMenuCommands.CheatRemoveCooldown.getMatcher(command) != null) {
      gm.disableCooldowns();
      System.out.println("All plant cooldowns cleared.");
    } else if (GamePlayMenuCommands.CheatUnlockAllPlants.getMatcher(command) != null) {
      GamePlayCheatHandler.unlockAllPlants(gm);
      saveUserState();
    } else if (GamePlayMenuCommands.ReleaseTheNuke.getMatcher(command) != null) {
      GamePlayCheatHandler.releaseTheNuke(gm);
    } else if (GamePlayMenuCommands.StartZombieWaves.getMatcher(command) != null) {
      try {
        gm.startZombieWaves();
        System.out.println("Zombie waves started.");
      } catch (Exception e) {
        System.out.println("error: could not start zombie waves. " + e.getMessage());
      }
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("GamePlay Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      return false;
    }
    return true;
  }

  private void handleAdvance(GameManager gm, Matcher m) {
    int count = parseInt(m.group("count"));
    if (count <= 0) {
      System.out.println("error: tick count must be a positive number");
      return;
    }
    for (int i = 0; i < count && gm.isRunning(); i++) {
      gm.advanceTime();
      printGameLogs(gm);
    }
    reportZombieKillsToQuests(gm);
    reportContextualQuestProgress(gm);
    if (!gm.isRunning()) {
      finishMatch(gm);
    }
  }

  private void printGameLogs(GameManager gm) {
    List<String> logs = gm.pollLogs();
    if (logs != null && !logs.isEmpty()) {
      for (String log : logs) {
        System.out.println("[event] " + log);
      }
    }
  }

  private void reportContextualQuestProgress(GameManager gm) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }
    MatchContext context = gm.getMatchContext();
    user.evaluateContextualQuests(context);
  }

  private void reportZombieKillsToQuests(GameManager gm) {
    int killed = gm.drainPendingKillCount();
    if (killed <= 0) {
      return;
    }
    User user = UserManager.getInstance().getCurrentUser();
    if (user != null) {
      user.triggerQuestEvent("KILL_ZOMBIE", killed);
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
    // ایمیتیتر خودش هیچ رفتاری ندارد؛ طبق داک اولین گیاه دیگر سیدبانک را کپی می‌کند تا همان کارت
    // با یک کول‌داون کاملا مستقل، دوباره قابل کاشت باشد
    String buildType = type;
    if (type.equalsIgnoreCase("imitater")) {
      buildType = resolveImitaterTarget(type);
      if (buildType == null) {
        System.out.println("error: Imitater needs another plant in your seed bank to copy");
        return;
      }
    }
    PlantTemplate template = GameDataManager.plantRepository.find(buildType);
    if (template == null) {
      System.out.println("error: unknown plant '" + type + "'");
      return;
    }

    int userLevel = getCurrentPlantLevel(buildType);
    int adjustedRecharge = adjustedRechargeSeconds(template, userLevel);
    int remaining = gm.ticksUntilPlantReady(type, adjustedRecharge);
    if (remaining > 0) {
      System.out.printf("error: %s is recharging; ready in %.1f seconds%n", type, remaining / 10.0);
      return;
    }

    Plant plant = null;
    try {
      plant = new PlantFactory(GameDataManager.plantRepository).createPlant(buildType, rc[0], rc[1], userLevel);
    } catch (RuntimeException e) {
      System.out.println("error: could not build plant '" + type + "'");
      return;
    }
    if (plant == null) {
      System.out.println("error: unknown plant '" + type + "'");
      return;
    }
    if (gm.placePlant(plant, rc[0], rc[1])) {
      gm.recordPlanting(type);
      System.out.printf("Planted %s at (%s, %s).%n", type, m.group("x"), m.group("y"));
      if (!buildType.equalsIgnoreCase(type)) {
        System.out.printf("%s imitates %s!%n", type, buildType);
      }
      activateBoostIfAny(plant, buildType);
      if (gm.getSpecialStageRule() != null && gm.getSpecialStageRule().belt() != null) {
        gm.getSpecialStageRule().belt().consumeReadyPlant();
      }
    } else {
      System.out.println("error: cannot plant there (tile occupied or not enough sun)");
    }
  }

  /** ایمیتیتر طبق داک اولین گیاه دیگر (غیر از خودش) را از سیدبانک بازیکن کپی می‌کند. */
  private String resolveImitaterTarget(String selfType) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return null;
    }
    for (String deckPlant : user.getSelectedDeck()) {
      if (deckPlant != null && !deckPlant.equalsIgnoreCase(selfType)) {
        return deckPlant;
      }
    }
    return null;
  }

  private int getCurrentPlantLevel(String plantType) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) return 1;
    return Math.max(1, user.getPlantLevel(plantType));
  }

  private int adjustedRechargeSeconds(PlantTemplate template, int level) {
    model.game.plant.PlantParts.PlantLevel levelStats =
            model.game.plant.PlantParts.PlantLevel.cumulative(template, level);
    return Math.max(0, template.recharge + levelStats.getCooldownDeltaSeconds());
  }

  private void activateBoostIfAny(Plant plant, String type) {
    for (String boosted : MatchSetup.getInstance().getBoostedPlants()) {
      if (boosted != null && boosted.equalsIgnoreCase(type)) {
        plant.applyPlantFood();
        System.out.println(type + " is boosted: plant food effect activated!");
        return;
      }
    }
  }

  private void handlePluck(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Plant plant = gm.getBoard().getTopPlantAt(rc[0], rc[1]);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    gm.getBoard().getPlants().remove(plant);
    System.out.printf("Plucked %s.%n", plant.getName());
  }

  private void handleCollectSun(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) return;

    Integer amount = gm.collectSunAt(rc[1], rc[0]);

    if (amount == null) {
      System.out.println("error: no sun to collect at that tile");
      return;
    }

    if (amount > 0) {
      User user = UserManager.getInstance().getCurrentUser();
      if (user != null) {
        user.triggerQuestEvent("COLLECT_SUN", amount);
      }
    }
  }

  private void handleFeed(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Plant plant = gm.getBoard().getTopPlantAt(rc[0], rc[1]);
    if (plant == null) {
      System.out.println("error: no plant at that tile");
      return;
    }
    gm.usePlantFood(plant);
  }


  private void handleSpawnZombie(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    GamePlayCheatHandler.spawnZombie(gm, m.group("zombieType").trim(), rc[0], rc[1]);
  }

  private void handlePlantsStatus(GameManager gm) {
    List<String> deck = MatchSetup.getInstance().getSelectedPlants();
    if (deck.isEmpty() || GameDataManager.plantRepository == null) {
      System.out.println("No plants selected for this match.");
      return;
    }
    // On a belt stage only the plant on the belt can go down, so say which one that is.
    if (gm.getSpecialStageRule() != null && gm.getSpecialStageRule().belt() != null) {
      String offer = gm.getSpecialStageRule().belt().peekReadyPlant();
      System.out.println("  Conveyor belt is offering: " + (offer == null ? "nothing yet" : offer));
    }
    for (String name : deck) {
      PlantTemplate template = GameDataManager.plantRepository.find(name);
      if (template == null) {
        continue;
      }
      int remaining = gm.ticksUntilPlantReady(name, adjustedRechargeSeconds(template, getCurrentPlantLevel(name)));
      String state = remaining == 0 ? "ready" : String.format("ready in %.1fs", remaining / 10.0);
      String cost =
              gm.isFreePlanting()
                      ? String.format("cost %d sun (FREE)", template.cost)
                      : String.format("cost %d sun", template.cost);
      System.out.printf("  %-20s - %-22s - %s%n", template.name, cost, state);
    }
  }

  private void handleTileStatus(GameManager gm, Matcher m) {
    int[] rc = parseCoord(gm.getBoard(), m.group("x"), m.group("y"));
    if (rc == null) {
      return;
    }
    Board board = gm.getBoard();
    Plant plant = board.getTopPlantAt(rc[0], rc[1]);
    Tile tile = board.getTile(rc[0], rc[1]);
    System.out.printf("Tile (%s, %s):%n", m.group("x"), m.group("y"));
    System.out.println(
            "  plant: " + (plant == null ? "none" : plant.getName() + " (hp " + plant.getCurrentHealth() + ")"));
    StringBuilder zs = new StringBuilder();
    for (Zombie z : board.getZombies()) {
      if (Math.round(z.getX()) == rc[1] && z.getRow() == rc[0] && !z.isDead()) {
        zs.append("\n    ").append(z.getName())
                .append(" [").append(view.BoardRenderer.typeOf(z)).append("]")
                .append(" (hp ").append(z.getCurrentHealth()).append("/").append(z.getMaxHealth())
                .append(", body damage ").append(z.getBodyDamageTaken())
                .append(", armour damage ").append(z.getArmorDamageTaken()).append(")")
                .append("\n      state: ").append(view.BoardRenderer.stateOf(z));
        for (var armor : z.getArmors()) {
          zs.append("\n      armor ").append(armor.getName())
                  .append(" [").append(armor.getType() == null ? "-" : armor.getType().name())
                  .append("]: ").append(armor.getCurrentHealth())
                  .append("/").append(armor.getMaxHealth())
                  .append(armor.isDestroyed() ? " BROKEN" : "");
        }
      }
    }
    System.out.println("  zombies: " + (zs.length() == 0 ? "none" : zs.toString()));
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
      System.out.printf("%s:%n", z.getDisplayName());
      System.out.printf("  position: %.1f, %d%n", z.getX() + 1, z.getRow() + 1);
      System.out.printf("  health: %d/%d   (body damage taken: %d)%n",
              z.getCurrentHealth(), z.getMaxHealth(), z.getBodyDamageTaken());
      printArmorInfo(z);
      System.out.println("  state: " + view.BoardRenderer.stateOf(z));
      System.out.println("  effects:");
      for (var effect : z.getActiveEffects().entrySet()) {
        System.out.printf(
                "    %s: %.1fs%n", effect.getKey().name().toLowerCase(), effect.getValue() / 10.0);
      }
      String ability = z.getBehavior() == null
              ? null : z.getBehavior().debugState(z, gm.getCurrentTick());
      if (ability != null) {
        System.out.println("  ability: " + ability);
      }
    }
  }

  /** لایه‌های زره را با نوع، جان باقی‌مانده و آسیب جذب‌شده چاپ می‌کند. */
  private void printArmorInfo(Zombie z) {
    System.out.println("  armor:");
    for (var armor : z.getArmors()) {
      System.out.printf("    %s [%s]: %d/%d (damage taken %d)%s%n",
              armor.getName(),
              armor.getType() == null ? "-" : armor.getType().name(),
              armor.getCurrentHealth(),
              armor.getMaxHealth(),
              armor.getMaxHealth() - armor.getCurrentHealth(),
              armor.isDestroyed() ? " BROKEN" : (armor.isMetallic() ? " (metallic)" : ""));
    }
    if (!z.getArmors().isEmpty()) {
      System.out.printf("    total: %d/%d (armor damage taken: %d, all broken: %s)%n",
              z.getRemainingArmorHealth(), z.getMaxArmorHealth(),
              z.getArmorDamageTaken(), z.isArmorBroken() ? "yes" : "no");
    }
  }
  private void finishMatch(GameManager gm) {
    // پیام برد/باخت را خود GameManager.endGame چاپ می‌کند
    MatchCompletion.apply(gm);
    Menu back = GameSession.getReturnMenu();
    GameSession.end();
    App.setCurrentMenu(back);
  }

  private void saveUserState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

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
