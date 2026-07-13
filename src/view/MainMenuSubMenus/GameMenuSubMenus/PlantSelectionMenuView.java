package view.MainMenuSubMenus.GameMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuSubControllers.PlantSelectionMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class PlantSelectionMenuView implements BaseMenu {
  private final PlantSelectionMenuController plantSelectionMenuController;

  public PlantSelectionMenuView() {
    this.plantSelectionMenuController = new PlantSelectionMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Plant Selection Menu. Select 6-7 plants for your Seed Bank. Use 'show all plants', "
            + "'show available plants', 'add plant -t <type>', 'remove plant -t <type>', "
            + "'boost plant -t <type>', 'start game', or 'menu exit'.");

    String input = scanner.nextLine();
    plantSelectionMenuController.handleinput(input);
  }
}
