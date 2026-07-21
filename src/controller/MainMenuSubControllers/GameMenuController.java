package controller.MainMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.account.AdventureMap;
import model.account.User;
import model.core.App;
import model.core.MatchSetup;
import model.enums.Commands.GameMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;

public class GameMenuController implements BaseController {
  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if ((matcher = MenuCommands.EnterMenu.getMatcher(command)) != null) {
      handleEnterMenu(matcher.group("menuName").trim().toLowerCase());
    } else if ((matcher = GameMenuCommands.EnterChapter.getMatcher(command)) != null) {
      handleEnterChapter(matcher.group("chapterName"));
    } else if (GameMenuCommands.GreenHouse.getMatcher(command) != null) {
      App.setCurrentMenu(Menu.GreenHouseMenu);
    } else if (GameMenuCommands.TravelLog.getMatcher(command) != null) {
      changeMenu("Travel Log", Menu.QuestMenu);
    } else if (GameMenuCommands.LeaderBoard.getMatcher(command) != null) {
      changeMenu("Leaderboard", Menu.ScoreBoardMenu);
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

  private void handleEnterMenu(String targetMenu) {
    switch (targetMenu) {
      case "collection menu", "collection" -> changeMenu("Collection Menu", Menu.CollectionMenu);
      default -> System.out.println("invalid menu!!!");
    }
  }

  private void changeMenu(String menuLabel, Menu target) {
    System.out.println("Changed to " + menuLabel + ".");
    App.setCurrentMenu(target);
  }

  private void handleEnterChapter(String chapterName) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      System.out.println("error: no user logged in");
      return;
    }

    Integer stageNumber = parseStageNumber(chapterName);
    if (stageNumber == null || stageNumber < 1 || stageNumber > AdventureMap.MAX_STAGES) {
      System.out.println(
              "error: unknown chapter \""
                      + chapterName
                      + "\" (valid chapters are 1-"
                      + AdventureMap.MAX_STAGES
                      + ")");
      return;
    }

    String stageKey = "stage_" + stageNumber;
    if (!user.getUnlockedStages().contains(stageKey)) {
      System.out.println(
              "error: chapter " + stageNumber + " is locked. Clear the previous chapter first.");
      return;
    }

    user.clearDeck();
    MatchSetup.getInstance().setTargetChapter(chapterName);

    System.out.println(
            "Entering " + chapterName + ". Choose your plants for the Seed Bank before starting.");
    changeMenu("Plant Selection Menu", Menu.PlantSelectionMenu);
  }

  private Integer parseStageNumber(String chapterName) {
    if (chapterName == null) {
      return null;
    }

    String normalized = chapterName.trim().toLowerCase();
    normalized = normalized.replaceFirst("^(chapter|stage)\\s*[-_ ]?", "").trim();

    try {
      return Integer.parseInt(normalized);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void handleShowWallet(String currency) {
    if (UserManager.getInstance().getCurrentUser() == null) {
      System.out.println("error: no user logged in");
      return;
    }
    System.out.println("Your " + currency + " balance will be shown here.");
  }

  private void handleCheatAdd(String countStr, String currency) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
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

    if ("coin".equalsIgnoreCase(currency)) {
      user.addCoins(count);
    } else if ("diamond".equalsIgnoreCase(currency)) {
      user.addDiamonds(count);
    } else {
      System.out.println("error: unknown currency type: " + currency);
      return;
    }

    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println("error: failed to save cheated currency: " + e.getMessage());
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