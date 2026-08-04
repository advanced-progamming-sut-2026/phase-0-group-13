package view;

import controller.MainMenuControllers;
import java.util.Scanner;

public class MainMenuView implements BaseMenu {
  private final MainMenuControllers mainMenuController;

  public MainMenuView() {
    this.mainMenuController = new MainMenuControllers();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Main Menu. Enter a sub-menu with 'menu enter <game/news/settings/profile> menu' or use"
            + " 'menu logout'."
            + unreadNewsBadge());

    String input = scanner.nextLine();
    mainMenuController.handleinput(input);
  }

  /** طبق داک، وقتی خبر خوانده‌نشده هست باید کنار دکمهٔ News یک نشان قرمز دیده شود. */
  private String unreadNewsBadge() {
    model.account.User user = data.persistence.UserManager.getInstance().getCurrentUser();
    if (user == null || user.getNewsBox() == null || user.getNewsBox().getUnreadCount() == 0) {
      return "";
    }
    return "  [NEWS *] " + user.getNewsBox().getUnreadCount() + " unread";
  }
}
