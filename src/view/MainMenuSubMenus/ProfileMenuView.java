package view.MainMenuSubMenus;

import controller.MainMenuSubControllers.ProfileMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class ProfileMenuView implements BaseMenu {
  private final ProfileMenuController profileMenuController;

  public ProfileMenuView() {
    this.profileMenuController = new ProfileMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Profile Menu. Use 'menu profile show-info', change-username/-nickname/-email/-password, or"
            + " 'menu exit' to go back.");

    String input = scanner.nextLine();
    profileMenuController.handleinput(input);
  }
}
