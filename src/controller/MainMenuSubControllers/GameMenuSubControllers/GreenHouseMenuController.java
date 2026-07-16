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
      handleCollect(matcher.group("x"), matcher.group("y"));
    } else if ((matcher = GreenHouseMenuCommands.Grow.getMatcher(command)) != null) {
      handleGrow(matcher.group("x"), matcher.group("y"));
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

  private int getIndexFromCoordinates(int x, int y) {
    if (x < 1 || x > 5 || y < 1 || y > 4) return -1;
    return (y - 1) * 5 + (x - 1);
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

      int x = (i % 5) + 1;
      int y = (i / 5) + 1;

      if (pot.isEmpty()) {
        System.out.println("  (" + x + ", " + y + ") empty");
      } else if (pot.isFullyGrown()) {
        System.out.println(
                "  (" + x + ", " + y + ") " + pot.getPlantedSeedId() + " - ready");
      } else {
        long remainingSec = pot.getRemainingGrowTime() / 1000;
        System.out.println(
                "  ("
                        + x
                        + ", "
                        + y
                        + ") "
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

    int x = parseIndex(xStr);
    int y = parseIndex(yStr);
    int index = getIndexFromCoordinates(x, y);

    if (index == -1) {
      System.out.println("error: coordinates out of bounds (x:1-5, y:1-4)");
      return;
    }

    Result result = user.getGreenHouse().plantSeed(index, user);
    System.out.println(result.message());
    saveIfSuccess(result);
  }

  private void handleCollect(String xStr, String yStr) {
    User user = requireUser();
    if (user == null) return;

    int x = parseIndex(xStr);
    int y = parseIndex(yStr);
    int index = getIndexFromCoordinates(x, y);

    if (index == -1) {
      System.out.println("error: coordinates out of bounds");
      return;
    }

    Result result = user.getGreenHouse().collectSeed(index);
    System.out.println(result.message());

    if (result.success()) {
      String seedId = (String) result.getObject();
      if ("marigold".equalsIgnoreCase(seedId)) {
        user.addCoins(500);
        System.out.println("You earned 500 coins from the Marigold!");
      } else {
        Result boostRes = user.addFreeBoost(seedId);
        System.out.println("Harvested a boost for " + seedId + ": " + boostRes.message());
      }
      saveIfSuccess(result);
    }
  }

  private void handleGrow(String xStr, String yStr) {
    User user = requireUser();
    if (user == null) return;

    int x = parseIndex(xStr);
    int y = parseIndex(yStr);
    int index = getIndexFromCoordinates(x, y);

    if (index == -1) {
      System.out.println("error: coordinates out of bounds");
      return;
    }

    Pot pot = user.getGreenHouse().getPotAt(index);
    if (pot == null || pot.isEmpty() || pot.isFullyGrown()) {
      System.out.println("error: invalid pot for growing");
      return;
    }

    long remainingMs = pot.getRemainingGrowTime();
    // محاسبه الماس‌ها: هر ساعت باقی‌مانده (حتی یک دقیقه) 1 الماس هزینه دارد
    int requiredDiamonds = (int) Math.ceil(remainingMs / (double) (60 * 60 * 1000));

    if (user.getDiamonds() < requiredDiamonds) {
      System.out.println("error: not enough diamonds. You need " + requiredDiamonds + " diamonds.");
      return;
    }

    user.addDiamonds(-requiredDiamonds);
    Result result = user.getGreenHouse().forceGrow(index);

    System.out.println(result.message());
    System.out.println("Spent " + requiredDiamonds + " diamonds to instantly grow the plant.");
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