package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.List;
import java.util.regex.Matcher;
import model.Result;
import model.account.User;
import model.core.App;
import model.enums.Commands.GreenHouseMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;
import model.environment.greenhouse.GreenHouse;
import model.environment.greenhouse.Pot;

public class GreenHouseMenuController implements BaseController {

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if (GreenHouseMenuCommands.ShowGreenHouse.getMatcher(command) != null) {
      handleShow();
    } else if ((matcher = GreenHouseMenuCommands.PlantPot.getMatcher(command)) != null) {
      handlePlant(matcher.group("x"), matcher.group("y"));
    } else if ((matcher = GreenHouseMenuCommands.Collect.getMatcher(command)) != null) {
      handleCollect(matcher.group("x"));
    } else if ((matcher = GreenHouseMenuCommands.Grow.getMatcher(command)) != null) {
      handleGrow(matcher.group("x"));
    } else if (GreenHouseMenuCommands.EnterShop.getMatcher(command) != null) {
      App.setCurrentMenu(Menu.ShopMenu);
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("GreenHouse Menu");
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

  private int parseIndex(String raw) {
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private void handleShow() {
    User user = requireUser();
    if (user == null) return;

    GreenHouse greenHouse = user.getGreenHouse();
    List<Pot> pots = greenHouse.getPots();
    System.out.println(
        "GreenHouse: "
            + greenHouse.getUnlockedPotsCount()
            + "/"
            + greenHouse.getMaxCapacity()
            + " pots unlocked.");

    for (int i = 0; i < pots.size(); i++) {
      Pot pot = pots.get(i);
      if (!pot.isUnlocked()) continue;

      if (pot.isEmpty()) {
        System.out.println("  [" + i + "] empty");
      } else if (pot.isFullyGrown()) {
        System.out.println("  [" + i + "] " + pot.getPlantedSeedId() + " - ready to collect!");
      } else {
        long remainingSec = pot.getRemainingGrowTime() / 1000;
        System.out.println(
            "  ["
                + i
                + "] "
                + pot.getPlantedSeedId()
                + " - growing ("
                + Math.round(pot.getGrowthProgress() * 100)
                + "%, ~"
                + remainingSec
                + "s left)");
      }
    }
  }

  private void handlePlant(String xStr, String yStr) {
    User user = requireUser();
    if (user == null) return;

    int index = parseIndex(xStr);
    String seedId = yStr;

    Result result = user.getGreenHouse().plantSeed(index, seedId);
    System.out.println(result.message());
    saveIfSuccess(result);
  }

  private void handleCollect(String xStr) {
    User user = requireUser();
    if (user == null) return;

    int index = parseIndex(xStr);
    Result result = user.getGreenHouse().collectSeed(index);
    System.out.println(result.message());

    if (result.success()) {
      String seedId = (String) result.getObject();
      user.getInventory().addItem("seed_" + seedId, 1);
      saveIfSuccess(result);
    }
  }

  private void handleGrow(String xStr) {
    User user = requireUser();
    if (user == null) return;

    int index = parseIndex(xStr);

    if (!user.getInventory().consumeItem("plant_food", 1)) {
      System.out.println("error: you don't have any plant food");
      return;
    }

    Result result = user.getGreenHouse().forceGrow(index);
    if (!result.success()) {
      user.getInventory().addItem("plant_food", 1);
    }
    System.out.println(result.message());
    saveIfSuccess(result);
  }

  private void saveIfSuccess(Result result) {
    if (!result.success()) return;
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
