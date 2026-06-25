package controller;

import data.persistence.UserManager;
import model.core.App;
import model.enums.Commands.MainMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;

import java.util.regex.Matcher;

public class MainMenuControllers implements BaseController {

    @Override
    public void initController() {
    }

    @Override
    public void handleinput(String input) {
        Matcher matcher;

        if ((matcher = MenuCommands.EnterMenu.getMatcher(input)) != null) {
            handleEnterMenu(matcher.group("menuName").trim().toLowerCase());
        }
        else if (MainMenuCommands.Logout.getMatcher(input) != null) {
            handleLogout();
        }
        else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
            System.out.println("Main Menu");
        }
        else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
            exit();
        }
        else {
            System.out.println("invalid input");
        }
    }

    private void handleEnterMenu(String targetMenu) {
        switch (targetMenu) {
            case "game menu", "game" -> changeMenu("Game Menu", Menu.GameMenu);
            case "news menu", "news" -> changeMenu("News Menu", Menu.NewsMenu);
            case "settings menu", "settings" -> changeMenu("Settings Menu", Menu.SettingsMenu);
            case "profile menu", "profile" -> changeMenu("Profile Menu", Menu.ProfileMenu);
            default -> System.out.println("invalid menu!!!");
        }
    }

    private void changeMenu(String menuLabel, Menu target) {
        System.out.println("Changed to " + menuLabel + ".");
        App.setCurrentMenu(target);
    }

    private void handleLogout() {
        UserManager.getInstance().logout();
        System.out.println("Logged out successfully.");
        App.setCurrentMenu(Menu.SignUpMenu);
    }

    @Override
    public void exit() {
        System.out.println("Use 'menu logout' to exit the Main Menu.");
    }
}
