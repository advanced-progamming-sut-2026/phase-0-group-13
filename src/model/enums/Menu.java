package model.enums;

import view.BaseMenu;
import view.MainMenuSubMenus.GameMenuView;
import view.MainMenuView;
import view.SignInMenuView;
import view.SignUpMenuView;

import java.util.Scanner;

public enum Menu {
    MainMenu(new MainMenuView()),
    GameMenu(new GameMenuView()),
    SignInMenu(new SignInMenuView()),
    SignUpMenu(new SignUpMenuView());

    private final BaseMenu menu;

    Menu(BaseMenu menu){this.menu = menu;}

    public void checkCommand(Scanner sc){this.menu.check(sc);}
}
