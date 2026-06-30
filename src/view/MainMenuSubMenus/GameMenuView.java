package view.MainMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class GameMenuView implements BaseMenu {
  private final GameMenuController gameMenuController;

  public GameMenuView() {
    this.gameMenuController = new GameMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Game Menu. Use 'menu enter chapter -c <name>', 'menu greenhouse', "
            + "'menu travel-log', 'menu leaderboard', 'menu coin-wallet', "
            + "'menu gem-wallet', 'menu cheat add <count> <coin|diamond>', "
            + "or 'menu exit'.");

    String input = scanner.nextLine();
    gameMenuController.handleinput(input);
  }
}
