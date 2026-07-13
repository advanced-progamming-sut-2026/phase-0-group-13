package view.MainMenuSubMenus.GameMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuSubControllers.GreenHouseMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class GreenHouseView implements BaseMenu {
  private final GreenHouseMenuController greenHouseMenuController;

  public GreenHouseView() {
    this.greenHouseMenuController = new GreenHouseMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "GreenHouse Menu. Use 'show greenhouse', 'plant pot at (<x>, <y>)', "
            + "'collect (<x>, <y>)', 'grow (<x>, <y>)', 'enter shop', or 'menu exit'.");

    String input = scanner.nextLine();
    greenHouseMenuController.handleinput(input);
  }
}
