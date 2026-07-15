package view;

import controller.SignInMenuController;
import java.util.Scanner;

public class SignInMenuView implements BaseMenu {
  private final SignInMenuController signInMenuController;

  public SignInMenuView() {
    this.signInMenuController = new SignInMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println("Entered Sign In Menu. Use 'login' to enter your account.");
    System.out.println("login format : \"login -u <username> " +
            "-p <password> -stay-logged-in\"");
    System.out.println("forget password format : \"forget password -u <username> " +
            "-e <email>\n\"");
      String input = scanner.nextLine();
    signInMenuController.handleinput(input);
  }
}
