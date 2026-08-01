package controller.MainMenuSubControllers.MiniGames;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.account.User;
import model.core.App;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchSetup;
import model.enums.Menu;
import model.enums.MiniGameType;
import model.game.minigame.arcade.BeghouledEngine;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.VasebreakerEngine;
import model.game.minigame.arcade.WallnutBowlingEngine;

public class MiniGameController implements BaseController {

  private static final int COIN_REWARD_PER_LEVEL = 150;
  private static final int MEOW_POINTS_ON_WIN = 200;

  private final Pattern smashVasePattern =
          Pattern.compile("(?i)^smash\\s+vase\\s+(?<x>\\d+)\\s+(?<y>\\d+)$");
  private final Pattern plantSeedPattern =
          Pattern.compile("(?i)^plant\\s+seed\\s+(?<type>[\\w-]+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)$");
  private final Pattern placeZombiePattern =
          Pattern.compile(
                  "(?i)^place\\s+zombie\\s+(?<type>[\\w-]+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)$");
  private final Pattern rollWalnutPattern =
          Pattern.compile("(?i)^roll\\s+(?<nut>walnut|explode-o-nut|giant-walnut)\\s+(?<lane>\\d+)$");
  private final Pattern advancePattern =
          Pattern.compile("(?i)^advance\\s+time\\s+(?<count>\\d+)\\s+ticks?$");
  private final Pattern swapPlantPattern =
          Pattern.compile("(?i)^swap\\s+plant\\s+(?<x1>\\d+)\\s+(?<y1>\\d+)\\s+(?<x2>\\d+)\\s+(?<y2>\\d+)$");
  private final Pattern upgradePlantPattern =
          Pattern.compile("(?i)^upgrade\\s+plant\\s+(?<type>[\\w-]+)$");

  private VasebreakerEngine vasebreaker;
  private WallnutBowlingEngine bowling;
  private IZombieEngine izombie;
  private BeghouledEngine beghouled;
  private MiniGameType activeType;
  private int activeLevel;
  private boolean finalized;

  @Override
  public void initController() {
    activeType = MatchSetup.getInstance().getCurrentMiniGame();
    activeLevel = MatchSetup.getInstance().getMiniGameLevel();
    finalized = false;

    switch (activeType) {
      case VASEBREAKER:
        vasebreaker = new VasebreakerEngine(activeLevel);
        System.out.println("Smash every vase before what's inside reaches your brain.");
        System.out.println("Commands: 'smash vase <x> <y>', 'plant seed <name> <x> <y>'.");
        break;
      case WALLNUT_BOWLING:
        bowling = new WallnutBowlingEngine(activeLevel);
        System.out.println("Bowl a nut down a lane and knock out every zombie.");
        System.out.println("Commands: 'roll walnut <lane>', 'roll explode-o-nut <lane>', "
                + "'roll giant-walnut <lane>'.");
        break;
      case I_ZOMBIE:
        izombie = new IZombieEngine(activeLevel);
        System.out.println("Deploy zombies to eat all 5 brains before your zombie-sun runs dry.");
        System.out.println("Command: 'place zombie <type> <x> <y>'.");
        System.out.println(izombie.renderMap());
        break;
      case BEGHOULED:
        beghouled = new BeghouledEngine(activeLevel);
        System.out.println("Line up three or more matching plants to earn sun and clear the lawn.");
        System.out.println("Commands: 'swap plant <x1> <y1> <x2> <y2>', 'upgrade plant <name>'.");
        System.out.println(beghouled.renderMap());
        break;
      default:
        System.out.println("error: unsupported mini-game type: " + activeType);
        break;
    }
  }

  @Override
  public void handleinput(String command) {
    if (!GameSession.hasActiveGame()) {
      System.out.println("error: no active mini-game. Return to the Game Menu.");
      exit();
      return;
    }

    if (activeType != MatchSetup.getInstance().getCurrentMiniGame()) {
      initController();
    }

    Matcher m;
    if ((m = advancePattern.matcher(command)).matches()) {
      handleAdvance(m);
    } else if (command.equalsIgnoreCase("show map")) {
      renderMap();
    } else if ((m = smashVasePattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.VASEBREAKER)) {
        handleSmashVase(m);
        checkForEnd();
      }
    } else if ((m = plantSeedPattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.VASEBREAKER)) {
        handlePlantSeed(m);
        checkForEnd();
      }
    } else if ((m = placeZombiePattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.I_ZOMBIE)) {
        handlePlaceZombie(m);
        checkForEnd();
      }
    } else if ((m = rollWalnutPattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.WALLNUT_BOWLING)) {
        handleRollWalnut(m);
        checkForEnd();
      }
    } else if ((m = swapPlantPattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.BEGHOULED)) {
        handleSwapPlant(m);
        checkForEnd();
      }
    } else if ((m = upgradePlantPattern.matcher(command)).matches()) {
      if (checkType(MiniGameType.BEGHOULED)) {
        System.out.println(beghouled.upgrade(m.group("type")));
        checkForEnd();
      }
    } else if (command.equalsIgnoreCase("exit")) {
      exit();
    } else {
      System.out.println("invalid input or command not permitted in this mini-game.");
    }
  }

  private boolean checkType(MiniGameType required) {
    if (activeType != required) {
      System.out.println("error: that command isn't available in " + activeType + ".");
      return false;
    }
    return true;
  }

  private void handleAdvance(Matcher m) {
    int count = Integer.parseInt(m.group("count"));
    for (int i = 0; i < count && !isCurrentEngineFinished(); i++) {
      tickActiveEngine();
    }
    checkForEnd();
  }

  private void tickActiveEngine() {
    switch (activeType) {
      case VASEBREAKER:
        vasebreaker.tick();
        break;
      case WALLNUT_BOWLING:
        bowling.tick();
        break;
      case I_ZOMBIE:
        izombie.tick();
        break;
      case BEGHOULED:
        beghouled.tick();
        break;
      default:
        break;
    }
  }

  private boolean isCurrentEngineFinished() {
    return switch (activeType) {
      case VASEBREAKER -> vasebreaker.isFinished();
      case WALLNUT_BOWLING -> bowling.isFinished();
      case I_ZOMBIE -> izombie.isFinished();
      case BEGHOULED -> beghouled.isFinished();
      default -> true;
    };
  }

  private void handleSwapPlant(Matcher m) {
    int x1 = Integer.parseInt(m.group("x1")) - 1;
    int y1 = Integer.parseInt(m.group("y1")) - 1;
    int x2 = Integer.parseInt(m.group("x2")) - 1;
    int y2 = Integer.parseInt(m.group("y2")) - 1;
    System.out.println(beghouled.swap(y1, x1, y2, x2));
  }

  private void handleSmashVase(Matcher m) {
    int x = Integer.parseInt(m.group("x")) - 1;
    int y = Integer.parseInt(m.group("y")) - 1;
    System.out.println(vasebreaker.smash(y, x));
  }

  private void handlePlantSeed(Matcher m) {
    String type = m.group("type");
    int x = Integer.parseInt(m.group("x")) - 1;
    int y = Integer.parseInt(m.group("y")) - 1;
    System.out.println(vasebreaker.plantSeed(type, y, x));
  }

  private void handlePlaceZombie(Matcher m) {
    String type = m.group("type");
    int x = Integer.parseInt(m.group("x")) - 1;
    int y = Integer.parseInt(m.group("y")) - 1;
    System.out.println(izombie.placeZombie(type, y, x));
  }

  private void handleRollWalnut(Matcher m) {
    int lane = Integer.parseInt(m.group("lane")) - 1;
    WallnutBowlingEngine.NutType type =
            switch (m.group("nut").toLowerCase()) {
              case "explode-o-nut" -> WallnutBowlingEngine.NutType.EXPLODE_O_NUT;
              case "giant-walnut" -> WallnutBowlingEngine.NutType.GIANT;
              default -> WallnutBowlingEngine.NutType.NORMAL;
            };
    System.out.println(bowling.rollWalnut(lane, type));
  }

  private void renderMap() {
    switch (activeType) {
      case VASEBREAKER -> System.out.print(vasebreaker.renderMap());
      case WALLNUT_BOWLING -> System.out.print(bowling.renderMap());
      case I_ZOMBIE -> System.out.print(izombie.renderMap());
      case BEGHOULED -> System.out.print(beghouled.renderMap());
      default -> System.out.println("--- Mini-Game Map (" + activeType + ") ---");
    }
  }

  private void checkForEnd() {
    if (finalized || !isCurrentEngineFinished()) {
      return;
    }
    finalized = true;

    boolean won = switch (activeType) {
      case VASEBREAKER -> vasebreaker.isWon();
      case WALLNUT_BOWLING -> bowling.isWon();
      case I_ZOMBIE -> izombie.isWon();
      case BEGHOULED -> beghouled.isWon();
      default -> false;
    };

    User user = UserManager.getInstance().getCurrentUser();
    if (won) {
      System.out.println("You cleared " + activeType + " (Level " + activeLevel + ")!");
      grantRewards(user);
    } else {
      System.out.println("The mini-game is over. Better luck next attempt at "
              + activeType + " (Level " + activeLevel + ").");
    }

    saveState();
    exit();
  }

  private void grantRewards(User user) {
    if (user == null) {
      return;
    }
    int coinReward = COIN_REWARD_PER_LEVEL * activeLevel;
    user.addCoins(coinReward);
    user.addMeowPoints(MEOW_POINTS_ON_WIN);
    System.out.println("Reward: +" + coinReward + " coins, +" + MEOW_POINTS_ON_WIN + " MyoPoints.");

    user.triggerQuestEvent("MINIGAME_CLEAR", 1);
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void exit() {
    Menu back = GameSession.getReturnMenu();
    System.out.println("Exiting Mini-Game... Returning to Travel Log.");
    GameSession.end();
    vasebreaker = null;
    bowling = null;
    izombie = null;
    beghouled = null;
    App.setCurrentMenu(back);
  }
}