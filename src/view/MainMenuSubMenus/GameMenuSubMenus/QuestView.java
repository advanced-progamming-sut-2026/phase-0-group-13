package view.MainMenuSubMenus.GameMenuSubMenus;

import controller.MainMenuSubControllers.GameMenuSubControllers.QuestMenuController;
import java.util.Scanner;
import view.BaseMenu;

public class QuestView implements BaseMenu {
  private final QuestMenuController questMenuController;

  public QuestView() {
    this.questMenuController = new QuestMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Travel Log. Use 'show quests', 'travel log page <category>', "
            + "'claim quest -t <title>', 'show mini-games', 'play mini-game -n <name>', "
            + "or 'menu exit'.");

    String input = scanner.nextLine();
    questMenuController.handleinput(input);
  }
}
