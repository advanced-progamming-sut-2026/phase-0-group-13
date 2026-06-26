package model.core;

import data.GameDataManager;
import java.util.Scanner;
import model.enums.Menu;
import view.MainMenuView;
import view.SignInMenuView;
import view.SignUpMenuView;

public class App {
  public static boolean shouldExit = false;
  private static Menu currentMenu = Menu.SignUpMenu;
  GameDataManager gameDataManager =
      new GameDataManager(); // تو اینجا میایم هرچی تو جیسون داریم و تبدیل به لیست میکنیم دوایی

  public static Menu getCurrentMenu() {
    return currentMenu;
  }

  public static void setCurrentMenu(Menu menu) {
    currentMenu = menu;
  }

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    while (!App.shouldExit) {
      if (App.getCurrentMenu() == Menu.SignUpMenu) {
        SignUpMenuView signUpView = new SignUpMenuView();
        signUpView.check(scanner);
      } else if (App.getCurrentMenu() == Menu.SignInMenu) {
        SignInMenuView signInView = new SignInMenuView();
        signInView.check(scanner);
      } else if (App.getCurrentMenu() == Menu.MainMenu) {
        MainMenuView mainView = new MainMenuView();
        mainView.check(scanner);
      }

      scanner.close();
    }
  }
}
