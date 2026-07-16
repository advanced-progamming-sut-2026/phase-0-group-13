package view;

import java.util.Scanner;
import data.persistence.UserManager;
import model.core.App;
import model.enums.Menu;

public class AppView {
  public static void run() {
    App.initData();

    if (UserManager.getInstance().restoreSession()) {
      System.out.println("Session restored. Welcome back, "
              + UserManager.getInstance().getCurrentUser().getUsername() + "!");
      App.setCurrentMenu(Menu.MainMenu);
    } else {
      App.setCurrentMenu(Menu.SignUpMenu);
    }

    Scanner scanner = new Scanner(System.in);
    do {
      if (!scanner.hasNextLine()) break;
      App.getCurrentMenu().checkCommand(scanner);
    } while (scanner.hasNextLine() && !App.shouldExit);
  }
}