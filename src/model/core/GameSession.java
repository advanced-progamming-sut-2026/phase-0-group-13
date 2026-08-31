package model.core;

import model.enums.Menu;

public final class GameSession {
  private static GameManager activeGame;
  private static Menu returnMenu = Menu.GameMenu;

  private GameSession() {}

  public static void start(GameManager gameManager, Menu menuToReturnTo) {
    activeGame = gameManager;
    returnMenu = menuToReturnTo;
  }

  public static GameManager getActiveGame() {
    return activeGame;
  }

  public static boolean hasActiveGame() {
    return activeGame != null;
  }

  public static Menu getReturnMenu() {
    return returnMenu;
  }

  public static void end() {
    activeGame = null;
  }
}
