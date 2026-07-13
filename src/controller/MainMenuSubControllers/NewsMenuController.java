package controller.MainMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.NewsMenuCommands;
import model.enums.Menu;
import model.game.news.AllNews;
import model.game.news.News;

public class NewsMenuController implements BaseController {

  @Override
  public void initController() {}

  @Override
  public void handleinput(String input) {
    if (NewsMenuCommands.ShowUnread.getMatcher(input) != null) {
      handleShowUnread();
    } else if (NewsMenuCommands.ShowAll.getMatcher(input) != null) {
      handleShowAll();
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(input) != null) {
      System.out.println("News Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(input) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private User requireUser() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      System.out.println("error: no user logged in");
    }
    return user;
  }

  private void handleShowUnread() {
    User user = requireUser();
    if (user == null) return;

    AllNews newsBox = user.getNewsBox();
    List<News> unread = newsBox.getUnreadNews();
    if (unread.isEmpty()) {
      System.out.println("You have no unread news.");
      return;
    }

    System.out.println("--- Unread News (" + unread.size() + ") ---");
    for (News news : new ArrayList<>(unread)) {
      System.out.println("  [" + news.getType() + "] " + news.getMessage());
    }

    // Opening the inbox marks everything in it as read.
    newsBox.markAllAsRead();
    saveState();
  }

  private void handleShowAll() {
    User user = requireUser();
    if (user == null) return;

    AllNews newsBox = user.getNewsBox();
    List<News> all = new ArrayList<>(newsBox.getUnreadNews());
    all.addAll(newsBox.getReadNews());

    if (all.isEmpty()) {
      System.out.println("You have no news yet.");
      return;
    }

    System.out.println("--- All News (" + all.size() + ") ---");
    for (News news : all) {
      String tag = news.isRead() ? "" : " [UNREAD]";
      System.out.println("  [" + news.getType() + "] " + news.getMessage() + tag);
    }
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
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