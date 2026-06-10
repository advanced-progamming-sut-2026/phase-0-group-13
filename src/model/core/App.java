package model.core;

import model.enums.Menu;

public class App {
    public static boolean shouldExit = false;
    private static Menu currentMenu = Menu.SignUpMenu;

    public static Menu getCurrentMenu(){return currentMenu;}
    public static void setCurrentMenu(Menu menu){currentMenu = menu;}
}
