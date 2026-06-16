package view;

import controller.AuthController;
import model.core.App;
import model.enums.Menu;
import java.util.Scanner;

public class SignUpMenuView implements BaseMenu {
    private final AuthController authController;

    public SignUpMenuView() {
        this.authController = new AuthController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("Entered Sign Up Menu. Use 'register' to create an account.");

        while (!App.shouldExit && App.getCurrentMenu() == Menu.SignUpMenu) {
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) continue;

            authController.handleinput(command);
        }
    }
}