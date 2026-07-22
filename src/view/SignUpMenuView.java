package view;

import controller.SignUpMenuController;
import java.util.Scanner;

public class SignUpMenuView implements BaseMenu {
  private final SignUpMenuController signUpController;

  public SignUpMenuView() {
    this.signUpController = new SignUpMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println("Entered Sign Up Menu. Use 'register' to create an account.");
    System.out.println("register format : \" register -u <username> " +
            "-p <password> <password_confirm> " +
            "-n <nickname> -e <email> -g <gender>\"");
    String input = scanner.nextLine();
    signUpController.handleinput(input);
  }
}
