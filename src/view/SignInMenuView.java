package view;

import controller.SignInMenuController;
import model.core.App;
import model.enums.Menu;
import java.util.Scanner;
import java.util.regex.Matcher;

public class SignInMenuView implements BaseMenu {
    private final SignInMenuController signInMenuController;

    public SignInMenuView() {
        this.signInMenuController = new SignInMenuController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("Entered Sign In Menu. Use 'login' to enter your account.");

        String input = scanner.nextLine();
        signInMenuController.handleinput(input);
    }
}