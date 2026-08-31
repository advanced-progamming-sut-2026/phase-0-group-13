package view;

import controller.MainMenuSubControllers.GameMenuSubControllers.GamePlayController;
import java.util.Scanner;

public class GamePlayView implements BaseMenu {
  private final GamePlayController controller = new GamePlayController();

  @Override
  public void check(Scanner scanner) {
    controller.handleinput(scanner.nextLine());
  }
}
