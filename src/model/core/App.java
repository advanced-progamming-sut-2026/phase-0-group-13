package model.core;

import data.GameDataManager;
import model.enums.Menu;

public class App {
    GameDataManager gameDataManager = new GameDataManager();// تو اینجا میایم هرچی تو جیسون داریم و تبدیل به لیست میکنیم دوایی
    public static boolean shouldExit = false;
    private static Menu currentMenu = Menu.SignUpMenu;

    public static Menu getCurrentMenu(){return currentMenu;}
    public static void setCurrentMenu(Menu menu){currentMenu = menu;}
}
