package controller.MainMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.SettingsMenuCommands;
import model.enums.Menu;

import java.util.regex.Matcher;

public class SettingsMenuController implements BaseController {

    @Override
    public void initController() {
    }

    @Override
    public void handleinput(String input) {
        Matcher matcher;

        if ((matcher = SettingsMenuCommands.ChangeDifficulty.getMatcher(input)) != null) {
            handleChangeDifficulty(matcher.group("difficultyLevel").trim());
        }
        else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
            System.out.println("Settings Menu");
        }
        else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
            exit();
        }
        else {
            System.out.println("invalid input");
        }
    }

    private void handleChangeDifficulty(String levelText) {
        int level;
        try {
            level = Integer.parseInt(levelText);
        } catch (NumberFormatException e) {
            System.out.println("error: difficulty level must be a number");
            return;
        }

        if (level < 1 || level > 5) {
            System.out.println("error: difficulty level must be between 1 and 5");
            return;
        }

        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.out.println("error: no user is currently logged in");
            return;
        }

        currentUser.setDifficultyLevel(level);
        try {
            UserManager.getInstance().updateCurrentUserGameState();
            System.out.println("Difficulty level changed to " + level + ".");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void exit() {
        System.out.println("Changed to Main Menu.");
        App.setCurrentMenu(Menu.MainMenu);
    }
}
