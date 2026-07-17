package controller;

import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.account.User;
import model.core.App;
import model.core.BonusGameLauncher;
import model.enums.Commands.MainMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;

public class MainMenuControllers implements BaseController {

  @Override
  public void initController() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user != null && user.getNewsBox().getUnreadCount() > 0) {
      System.out.println("\n[!] You have " + user.getNewsBox().getUnreadCount()
              + " unread news items. Check the News Menu!\n");
    }
  }

  @Override
  public void handleinput(String input) {
    Matcher matcher;

    if ((matcher = MenuCommands.EnterMenu.getMatcher(input)) != null) {
      handleEnterMenu(matcher.group("menuName").trim().toLowerCase());
    } else if (MainMenuCommands.Logout.getMatcher(input) != null) {
      handleLogout();
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
      User user = UserManager.getInstance().getCurrentUser();
      boolean hasUnread = user != null && user.getNewsBox().getUnreadCount() > 0;

      System.out.println("Main Menu");
      System.out.println("Available Menus: Game, Profile, Settings, News" + (hasUnread ? " (!)" : ""));
    } else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleEnterMenu(String targetMenu) {
    switch (targetMenu) {
      case "game menu", "game" -> changeMenu("Game Menu", Menu.GameMenu);
      case "news menu", "news" -> changeMenu("News Menu", Menu.NewsMenu);
      case "settings menu", "settings" -> changeMenu("Settings Menu", Menu.SettingsMenu);
      case "profile menu", "profile" -> changeMenu("Profile Menu", Menu.ProfileMenu);
      case "bonus game", "game bonus" -> handleEnterBonusGame();
      default -> System.out.println("invalid menu!!!");
    }
  }

  private void handleEnterBonusGame() {
    if (UserManager.getInstance().getCurrentUser() == null) {
      System.out.println("error: no user logged in");
      return;
    }
    BonusGameLauncher.launch();
  }

  private void changeMenu(String menuLabel, Menu target) {
    System.out.println("Changed to " + menuLabel + ".");
    App.setCurrentMenu(target);
  }

  private void handleLogout() {
    UserManager.getInstance().logout();
    System.out.println("Logged out successfully.");
    App.setCurrentMenu(Menu.SignUpMenu);
  }

  @Override
  public void exit() {
    System.out.println("Use 'menu logout' to exit the Main Menu.");
  }
}