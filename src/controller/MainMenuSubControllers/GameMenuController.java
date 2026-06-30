package controller.MainMenuSubControllers;

import java.util.regex.Matcher;
import controller.BaseController;
import data.persistence.UserManager;
import model.core.App;
import model.enums.Commands.GameMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;

public class GameMenuController implements BaseController {
  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if ((matcher = GameMenuCommands.EnterChapter.getMatcher(command)) != null) {
      handleEnterChapter(matcher.group("chapterName"));
    } else if (GameMenuCommands.GreenHouse.getMatcher(command) != null) {
      System.out.println("Greenhouse menu is not available yet.");
    } else if (GameMenuCommands.TravelLog.getMatcher(command) != null) {
      System.out.println("Travel log is not available yet.");
    } else if (GameMenuCommands.LeaderBoard.getMatcher(command) != null) {
      System.out.println("Leaderboard is not available yet.");
    } else if (GameMenuCommands.CoinWallet.getMatcher(command) != null) {
      handleShowWallet("coin");
    } else if (GameMenuCommands.GemWallet.getMatcher(command) != null) {
      handleShowWallet("diamond");
    } else if ((matcher = GameMenuCommands.CheatAdd.getMatcher(command)) != null) {
      handleCheatAdd(matcher.group("count"), matcher.group("currency"));
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Game Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleEnterChapter(String chapterName) {
    System.out.println("Starting chapter: " + chapterName);
  }

  private void handleShowWallet(String currency) {
    if (UserManager.getInstance().getCurrentUser() == null) {
      System.out.println("error: no user logged in");
      return;
    }
    System.out.println("Your " + currency + " balance will be shown here.");
  }

  private void handleCheatAdd(String countStr, String currency) {
    if (UserManager.getInstance().getCurrentUser() == null) {
      System.out.println("error: no user logged in");
      return;
    }

    int count;
    try {
      count = Integer.parseInt(countStr);
    } catch (NumberFormatException e) {
      System.out.println("error: invalid count");
      return;
    }

    System.out.println("Added " + count + " " + currency + "(s).");
  }

  @Override
  public void exit() {
    System.out.println("Changed to Main Menu.");
    App.setCurrentMenu(Menu.MainMenu);
  }
}
