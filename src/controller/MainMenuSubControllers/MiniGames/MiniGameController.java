package controller.MainMenuSubControllers.MiniGames;

import controller.BaseController;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.core.App;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchSetup;
import model.enums.Menu;
import model.enums.MiniGameType;

public class MiniGameController implements BaseController {

  private final Pattern smashVasePattern = Pattern.compile("(?i)^smash\\s+vase\\s+(?<x>\\d+)\\s+(?<y>\\d+)$");
  private final Pattern placeZombiePattern = Pattern.compile("(?i)^place\\s+zombie\\s+" +
          "(?<type>[\\w\\-]+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)$");
  private final Pattern advancePattern = Pattern.compile("(?i)^advance\\s+time\\s+(?<count>\\d+)\\s+ticks?$");

  @Override
  public void initController() {
    MiniGameType type = MatchSetup.getInstance().getCurrentMiniGame();
    int level = MatchSetup.getInstance().getMiniGameLevel();
    System.out.println("--- Mini-Game Started ---");
    System.out.println("Mode: " + type + " | Level: " + level);
  }

  @Override
  public void handleinput(String command) {
    GameManager gm = GameSession.getActiveGame();
    if (gm == null) {
      System.out.println("error: no active mini-game. Return to the Game Menu.");
      exit();
      return;
    }

    MiniGameType type = MatchSetup.getInstance().getCurrentMiniGame();
    Matcher m;

    if ((m = advancePattern.matcher(command)).matches()) {
      handleAdvance(gm, m);
    } else if (command.equalsIgnoreCase("show map")) {
      System.out.println("--- Mini-Game Map (" + type + ") ---");
    } else if ((m = smashVasePattern.matcher(command)).matches()) {
      if (type != MiniGameType.VASEBREAKER) {
        System.out.println("error: 'smash vase' command is only available in Vasebreaker.");
      } else {
        handleSmashVase(gm, m);
      }
    } else if ((m = placeZombiePattern.matcher(command)).matches()) {
      if (type != MiniGameType.I_ZOMBIE) {
        System.out.println("error: 'place zombie' command is only available in I-Zombie.");
      } else {
        handlePlaceZombie(gm, m);
      }
    } else if (command.equalsIgnoreCase("exit")) {
      exit();
    } else {
      System.out.println("invalid input or command not permitted in this mini-game.");
    }
  }

  private void handleAdvance(GameManager gm, Matcher m) {
    int count = Integer.parseInt(m.group("count"));
    for(int i = 0; i < count && gm.isRunning(); i++) {
      gm.advanceTime();
    }
    if (!gm.isRunning()) {
      System.out.println("Mini-game ended!");
      exit();
    }
  }

  private void handleSmashVase(GameManager gm, Matcher m) {
    String x = m.group("x");
    String y = m.group("y");
    System.out.printf("Smashed vase at (%s, %s).%n", x, y);
    // TODO: اگر آیتمی افتاد، آن را به عنوان Reward یا Zombie اعمال کنید
  }

  private void handlePlaceZombie(GameManager gm, Matcher m) {
    String type = m.group("type");
    String x = m.group("x");
    String y = m.group("y");
    System.out.printf("You placed a %s zombie at (%s, %s).%n", type, x, y);
  }

  @Override
  public void exit() {
    System.out.println("Exiting Mini-Game... Returning to Game Menu.");
    GameSession.end();
    App.setCurrentMenu(Menu.GameMenu);
  }
}