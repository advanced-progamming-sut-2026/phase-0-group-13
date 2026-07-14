package view.MainMenuSubMenus.GameMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuSubControllers.ScoreBoardMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class ScoreBoardMenuView implements BaseMenu {
  private final ScoreBoardMenuController scoreBoardMenuController;

  public ScoreBoardMenuView() {
    this.scoreBoardMenuController = new ScoreBoardMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Leaderboard. Use 'sort by high score', 'sort by last passed level', "
            + "'sort by mini games', 'sort by quests', 'sort by daily quests', "
            + "or 'menu exit'.");

    String input = scanner.nextLine();
    scoreBoardMenuController.handleinput(input);
  }
}
