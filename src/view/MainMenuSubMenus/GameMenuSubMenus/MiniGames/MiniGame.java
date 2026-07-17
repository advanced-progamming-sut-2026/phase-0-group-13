package view.MainMenuSubMenus.GameMenuSubMenus.MiniGames;

import controller.MainMenuSubControllers.MiniGames.MiniGameController;
import java.util.Scanner;
import view.BaseMenu;

public class MiniGame implements BaseMenu {
  private final MiniGameController miniGameController = new MiniGameController();

  @Override
  public void check(Scanner scanner) {
    miniGameController.handleinput(scanner.nextLine());
  }
}