package model.core;

import model.enums.Menu;

public class Router {
    private Menu currentMenu;
    private Menu previousMenu;
    private static final Router instance = new Router();

    private Router() {
    }

    public static Router getInstance() {
        return instance;
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }

    public Menu getPreviousMenu() {
        return previousMenu;
    }

    public void enterMenu() {
    }

    public void enterMenu(Menu menu) {
        previousMenu = currentMenu;
        currentMenu = menu;
    }

    public void exitMenu() {
    }

    public void exitMenu(Menu fallback) {
        previousMenu = currentMenu;
        currentMenu = fallback;
    }

    public void showCurrentMenu() {
    }
}
