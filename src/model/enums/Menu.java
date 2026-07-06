package model.enums;

import java.util.Scanner;
import view.*;
import view.MainMenuSubMenus.GameMenuSubMenus.GreenHouseView;
import view.MainMenuSubMenus.GameMenuSubMenus.ShopView;
import view.MainMenuSubMenus.GameMenuView;
import view.MainMenuSubMenus.NewsMenuView;
import view.MainMenuSubMenus.ProfileMenuView;
import view.MainMenuSubMenus.SettingsMenuView;

public enum Menu {
  MainMenu(new MainMenuView()),
  GameMenu(new GameMenuView()),
  ShopMenu(new ShopView()),
  GreenHouseMenu(new GreenHouseView()),
  CollectionMenu(new CollectionMenuView()),
  PlantSelectionMenu(new PlantSelectionMenuView()),
  NewsMenu(new NewsMenuView()),
  SettingsMenu(new SettingsMenuView()),
  ProfileMenu(new ProfileMenuView()),
  SignInMenu(new SignInMenuView()),
  SignUpMenu(new SignUpMenuView());

  private final BaseMenu menu;

  Menu(BaseMenu menu) {
    this.menu = menu;
  }

  public void checkCommand(Scanner sc) {
    this.menu.check(sc);
  }
}
