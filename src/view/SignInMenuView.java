package view;

import controller.AuthController;
import model.core.App;
import model.enums.Menu;
import java.util.Scanner;

public class SignInMenuView implements BaseMenu {
    private final AuthController authController;

    public SignInMenuView() {
        this.authController = new AuthController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("Entered Sign In Menu. Use 'login' to enter your account.");

        while (!App.shouldExit && App.getCurrentMenu() == Menu.SignInMenu) {
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) continue;

            authController.handleinput(command);
        }
    }
}