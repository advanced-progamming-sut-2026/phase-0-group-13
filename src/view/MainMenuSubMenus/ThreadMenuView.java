package view.MainMenuSubMenus;

import java.util.Scanner;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;
import view.BaseMenu;

public class ThreadMenuView implements BaseMenu {
  @Override
  public void check(Scanner scanner) {
    System.out.println("This menu isn't available yet. Use 'menu exit' to go back.");

    String input = scanner.nextLine();
    if (MenuCommands.ExitMenu.getMatcher(input) != null) {
      System.out.println("Changed to Main Menu.");
      App.setCurrentMenu(Menu.MainMenu);
    } else {
      System.out.println("invalid input");
    }
  }
}
