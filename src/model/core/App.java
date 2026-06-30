package model.core;

import data.GameDataManager;
import model.enums.Menu;

public class App {
  public static boolean shouldExit = false;
  private static Menu currentMenu = Menu.SignUpMenu;
  private static final GameDataManager gameDataManager =
      new GameDataManager(); // تو اینجا میایم هرچی تو جیسون داریم و تبدیل به لیست میکنیم دوایی

  public static Menu getCurrentMenu() {
    return currentMenu;
  }

  public static void setCurrentMenu(Menu menu) {
    currentMenu = menu;
  }

  public static void initData() {
      gameDataManager.initAllData();
  }
}
