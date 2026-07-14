package model.enums;

import java.util.Scanner;
import view.*;
import view.MainMenuSubMenus.*;
import view.MainMenuSubMenus.GameMenuSubMenus.*;

public enum Menu {
  MainMenu(new MainMenuView()),
  GameMenu(new GameMenuView()),
  ShopMenu(new ShopView()),
  GreenHouseMenu(new GreenHouseView()),
  CollectionMenu(new CollectionMenuView()),
  PlantSelectionMenu(new PlantSelectionMenuView()),
  GamePlayMenu(new GamePlayView()),
  QuestMenu(new QuestView()),
  ScoreBoardMenu(new ScoreBoardMenuView()),
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
