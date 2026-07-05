package view;

import controller.MainMenuSubControllers.GameMenuSubControllers.CollectionMenuController;
import java.util.Scanner;

public class CollectionMenuView implements BaseMenu {
  private final CollectionMenuController collectionMenuController;

  public CollectionMenuView() {
    this.collectionMenuController = new CollectionMenuController();
  }

  @Override
  public void check(Scanner scanner) {
    System.out.println(
        "Collection Menu. Use 'menu collection show-plants', 'show-all-plants', "
            + "'show-zombies', 'show-all-zombies', 'show-plant -p <name>', "
            + "'show-zombie -z <name>', 'upgrade-plant -p <name>', "
            + "'purchase-plant -p <name>', or 'menu exit'.");

    String input = scanner.nextLine();
    collectionMenuController.handleinput(input);
  }
}
