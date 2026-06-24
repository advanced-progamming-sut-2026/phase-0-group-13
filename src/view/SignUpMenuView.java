package view;

import controller.SignUpMenuController;
import model.core.App;
import model.enums.Menu;
import java.util.Scanner;

public class SignUpMenuView implements BaseMenu {
    private final SignUpMenuController signUpController;

    public SignUpMenuView() {
        this.signUpController = new SignUpMenuController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("Entered Sign Up Menu. Use 'register' to create an account.");

        String input = scanner.nextLine();
        signUpController.handleinput(input);
    }
}