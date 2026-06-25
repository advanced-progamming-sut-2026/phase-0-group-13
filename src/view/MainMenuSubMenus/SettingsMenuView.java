package view.MainMenuSubMenus;

import controller.MainMenuSubControllers.SettingsMenuController;
import view.BaseMenu;

import java.util.Scanner;

public class SettingsMenuView implements BaseMenu {
    private final SettingsMenuController settingsMenuController;

    public SettingsMenuView() {
        this.settingsMenuController = new SettingsMenuController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("Settings Menu. Use 'menu settings change-difficulty -l <level>' (1-5). Use 'menu exit' to go back.");

        String input = scanner.nextLine();
        settingsMenuController.handleinput(input);
    }
}
