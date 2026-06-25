package controller.MainMenuSubControllers;

import controller.BaseController;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.NewsMenuCommands;
import model.enums.Menu;

public class NewsMenuController implements BaseController {

    @Override
    public void initController() {
    }

    @Override
    public void handleinput(String input) {
        if (NewsMenuCommands.ShowUnread.getMatcher(input) != null) {
            handleShowUnread();
        }
        else if (NewsMenuCommands.ShowAll.getMatcher(input) != null) {
            handleShowAll();
        }
        else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
            System.out.println("News Menu");
        }
        else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
            exit();
        }
        else {
            System.out.println("invalid input");
        }
    }

    private void handleShowUnread() {
        System.out.println("You have no unread news.");
    }

    private void handleShowAll() {
        System.out.println("You have no news yet.");
    }

    @Override
    public void exit() {
        System.out.println("Changed to Main Menu.");
        App.setCurrentMenu(Menu.MainMenu);
    }
}
