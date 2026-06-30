package view.MainMenuSubMenus.GameMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuSubControllers.ShopMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class ShopView implements BaseMenu {
  private final ShopMenuController shopMenuController;

  public ShopView() {
    this.shopMenuController = new ShopMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Shop Menu. Use 'shop list', 'shop daily', "
            + "'shop buy -i <itemId> -n <count> [-t <plantType>]', or 'menu exit'.");

    String input = scanner.nextLine();
    shopMenuController.handleinput(input);
  }
}
