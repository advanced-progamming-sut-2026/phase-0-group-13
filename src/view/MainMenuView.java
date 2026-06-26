package view;

import controller.MainMenuControllers;
import java.util.Scanner;

public class MainMenuView implements BaseMenu {
  private final MainMenuControllers mainMenuController;

  public MainMenuView() {
    this.mainMenuController = new MainMenuControllers();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Main Menu. Enter a sub-menu with 'menu enter <game/news/settings/profile> menu' or use"
            + " 'menu logout'.");

    String input = scanner.nextLine();
    mainMenuController.handleinput(input);
  }
}
