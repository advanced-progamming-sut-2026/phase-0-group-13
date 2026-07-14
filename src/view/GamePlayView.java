package view;

import controller.MainMenuSubControllers.GameMenuSubControllers.GamePlayController;
import java.util.Scanner;

/**
 * In-match view. Unlike the hub menus it prints no banner per line (the player types many commands
 * in a row); it just forwards each line to the {@link GamePlayController}.
 */
public class GamePlayView implements BaseMenu {
  private final GamePlayController controller = new GamePlayController();

  @Override
  public void check(Scanner scanner) {
    controller.handleinput(scanner.nextLine());
  }
}
