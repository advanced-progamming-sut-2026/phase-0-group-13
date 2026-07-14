package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.regex.Matcher;
import model.Result;
import model.account.User;
import model.core.App;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.PlantSelectionMenuCommands;
import model.enums.Menu;

public class PlantSelectionMenuController implements BaseController {

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if (PlantSelectionMenuCommands.ShowAllPlants.getMatcher(command) != null) {
      handleShowAllPlants();
    } else if (PlantSelectionMenuCommands.ShowAvailablePlants.getMatcher(command) != null) {
      handleShowAvailablePlants();
    } else if ((matcher = PlantSelectionMenuCommands.AddPlant.getMatcher(command)) != null) {
      handleAddPlant(matcher.group("type").trim());
    } else if ((matcher = PlantSelectionMenuCommands.RemovePlant.getMatcher(command)) != null) {
      handleRemovePlant(matcher.group("type").trim());
    } else if ((matcher = PlantSelectionMenuCommands.BoostPlant.getMatcher(command)) != null) {
      handleBoostPlant(matcher.group("type").trim());
    } else if (PlantSelectionMenuCommands.StartGame.getMatcher(command) != null) {
      handleStartGame();
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Plant Selection Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private User requireUser() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      System.out.println("error: no user logged in");
    }
    return user;
  }

  private void handleShowAllPlants() {
    User user = requireUser();
    if (user == null) return;

    System.out.println("--- All Unlocked Plants ---");
    for (String name : user.getUnlockedPlants()) {
      boolean selected = user.getSelectedDeck().contains(name);
      boolean boosted = user.isPlantBoosted(name);
      System.out.println(
              "  " + name + (selected ? " [selected]" : "") + (boosted ? " [boosted]" : ""));
    }
  }

  private void handleShowAvailablePlants() {
    User user = requireUser();
    if (user == null) return;

    System.out.println("--- Available Plants for This Stage ---");
    for (String name : user.getUnlockedPlants()) {
      if (!user.getSelectedDeck().contains(name)) {
        System.out.println("  " + name);
      }
    }
  }

  private void handleAddPlant(String type) {
    User user = requireUser();
    if (user == null) return;

    Result result = user.addToDeck(type);
    System.out.println(result.message());
  }

  private void handleRemovePlant(String type) {
    User user = requireUser();
    if (user == null) return;

    Result result = user.removeFromDeck(type);
    System.out.println(result.message());
  }

  private void handleBoostPlant(String type) {
    User user = requireUser();
    if (user == null) return;

    Result result = user.boostPlant(type);
    System.out.println(result.message());

    if (result.success()) {
      saveState();
    }
  }

  private void handleStartGame() {
    User user = requireUser();
    if (user == null) return;

    int deckSize = user.getSelectedDeck().size();
    if (deckSize < User.MIN_DECK_SLOTS) {
      System.out.println(
              "error: select at least "
                      + User.MIN_DECK_SLOTS
                      + " plants before starting the game (you have "
                      + deckSize
                      + "/"
                      + User.MIN_DECK_SLOTS
                      + ")");
      return;
    }

    MatchSetup.getInstance().setSelectedPlants(user.getSelectedDeck());
    MatchSetup.getInstance().setBoostedPlants(user.getBoostedPlants());
    MatchSetup.getInstance().setDifficultyLevel(user.getDifficultyLevel());

    System.out.println("Seed Bank locked in: " + user.getSelectedDeck());
    System.out.println("Handing off to the game engine...");
    MatchLauncher.launch();
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void exit() {
    System.out.println("Changed to Game Menu.");
    App.setCurrentMenu(Menu.GameMenu);
  }
}