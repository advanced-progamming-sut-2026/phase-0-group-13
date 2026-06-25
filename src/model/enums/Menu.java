package model.enums;

import view.BaseMenu;
import view.MainMenuSubMenus.GameMenuView;
import view.MainMenuSubMenus.NewsMenuView;
import view.MainMenuSubMenus.ProfileMenuView;
import view.MainMenuSubMenus.SettingsMenuView;
import view.MainMenuView;
import view.SignInMenuView;
import view.SignUpMenuView;

import java.util.Scanner;

public enum Menu {
    MainMenu(new MainMenuView()),
    GameMenu(new GameMenuView()),
    NewsMenu(new NewsMenuView()),
    SettingsMenu(new SettingsMenuView()),
    ProfileMenu(new ProfileMenuView()),
    SignInMenu(new SignInMenuView()),
    SignUpMenu(new SignUpMenuView());

    private final BaseMenu menu;

    Menu(BaseMenu menu){this.menu = menu;}

    public void checkCommand(Scanner sc){this.menu.check(sc);}
}
