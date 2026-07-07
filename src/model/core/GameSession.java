package model.core;

import model.enums.Menu;

/**
 * Holds the single active match so that the menu layer (which creates the match) and the gameplay
 * controller (which drives it) can share one {@link GameManager} instance across separate command
 * invocations. There is at most one match in progress at a time.
 */
public final class GameSession {
  private static GameManager activeGame;
  private static Menu returnMenu = Menu.GameMenu;

  private GameSession() {}

  /** Starts a new match and remembers which menu to return to when it ends. */
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

  /** Clears the active match once it is won, lost, or exited. */
  public static void end() {
    activeGame = null;
  }
}
