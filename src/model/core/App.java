package model.core;

import data.GameDataManager;
import model.enums.Menu;

public class App {
  public static boolean shouldExit = false;
  private static Menu currentMenu = Menu.SignUpMenu;
  // تو اینجا میایم هرچی تو جیسون داریم و تبدیل به لیست میکنیم دوایی
  private static final GameDataManager GAME_DATA_MANAGER = new GameDataManager();

  public static Menu getCurrentMenu() {
    return currentMenu;
  }

  public static void setCurrentMenu(Menu menu) {
    currentMenu = menu;
  }

  public static void initData() {
    GAME_DATA_MANAGER.initAllData();

    // Both front ends come through here, so this is the one place the client connects. A failure
    // is not fatal: the menus still open and the account layer reports it when something actually
    // needs the server.
    network.client.ClientSession.getInstance().connect();

    if (GameDataManager.wasSessionRestored()) {
      currentMenu = Menu.MainMenu;
    }
  }
}
