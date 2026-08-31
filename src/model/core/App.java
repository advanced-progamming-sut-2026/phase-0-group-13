package model.core;

import data.GameDataManager;
import model.enums.Menu;

public class App {
  public static boolean shouldExit = false;
  private static Menu currentMenu = Menu.SignUpMenu;
  private static final GameDataManager GAME_DATA_MANAGER = new GameDataManager();

  public static Menu getCurrentMenu() {
    return currentMenu;
  }

  public static void setCurrentMenu(Menu menu) {
    currentMenu = menu;
  }

  public static void initData() {
    GAME_DATA_MANAGER.initAllData();

    network.client.ClientSession.getInstance().connect();

    if (GameDataManager.wasSessionRestored()) {
      currentMenu = Menu.MainMenu;
    }
  }
}
