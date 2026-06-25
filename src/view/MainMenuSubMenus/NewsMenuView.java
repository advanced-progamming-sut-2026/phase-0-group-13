package view.MainMenuSubMenus;

import controller.MainMenuSubControllers.NewsMenuController;
import view.BaseMenu;

import java.util.Scanner;

public class NewsMenuView implements BaseMenu {
    private final NewsMenuController newsMenuController;

    public NewsMenuView() {
        this.newsMenuController = new NewsMenuController();
    }

    @Override
    public void check(Scanner scanner) {
        System.out.println("News Menu. Use 'menu news show-unread' or 'menu news show-all'. Use 'menu exit' to go back.");

        String input = scanner.nextLine();
        newsMenuController.handleinput(input);
    }
}
