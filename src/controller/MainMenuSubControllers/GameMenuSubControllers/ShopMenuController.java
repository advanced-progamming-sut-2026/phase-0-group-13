package controller.MainMenuSubControllers.GameMenuSubControllers;

import java.util.regex.Matcher;
import controller.BaseController;
import data.persistence.UserManager;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.ShopMenuCommands;
import model.enums.CurrencyType;
import model.enums.Menu;
import model.game.shop.Shop;
import model.game.shop.ShopItem;

public class ShopMenuController implements BaseController {
  private final Shop shop;

  public ShopMenuController() {
    this.shop = new Shop();
  }

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if (ShopMenuCommands.List.getMatcher(command) != null) {
      handleList();
    } else if (ShopMenuCommands.Daily.getMatcher(command) != null) {
      handleDaily();
    } else if ((matcher = ShopMenuCommands.Buy.getMatcher(command)) != null) {
      String countStr = matcher.group("count");
      int count;
      try {
        count = Integer.parseInt(countStr);
      } catch (NumberFormatException e) {
        System.out.println("error: count must be a number");
        return;
      }
      handleBuy(matcher.group("itemId"), count, matcher.group("plantType"));
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Shop Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleList() {
    System.out.println("--- All-Time Products ---");
    for (ShopItem item : shop.getAllTimeProducts()) {
      printItem(item);
    }
  }

  private void handleDaily() {
    System.out.println("--- Daily Products ---");
    for (ShopItem item : shop.getDailyTimeProducts()) {
      printItem(item);
    }
  }

  private void printItem(ShopItem item) {
    String currency = item.getCurrencyType() == CurrencyType.COIN ? "coins" : "diamonds";
    System.out.println(item.getId() + " - " + item.getPrice() + " " + currency);
  }

  private void handleBuy(String itemId, int count, String plantType) {
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      System.out.println("error: no user logged in");
      return;
    }

    if (count <= 0) {
      System.out.println("error: count must be positive");
      return;
    }

    ShopItem item = findItem(itemId);
    if (item == null) {
      System.out.println("error: item not found");
      return;
    }

    if (!item.isAvailable()) {
      System.out.println("error: item out of stock");
      return;
    }

    int totalCost = item.getPrice() * count;
    if (item.getCurrencyType() == CurrencyType.COIN) {
      if (currentUser.getCoins() < totalCost) {
        System.out.println("error: not enough coins");
        return;
      }
      currentUser.addCoins(-totalCost);
    } else {
      if (currentUser.getDiamonds() < totalCost) {
        System.out.println("error: not enough diamonds");
        return;
      }
      currentUser.addDiamonds(-totalCost);
    }

    item.decreaseStock();
    currentUser.getInventory().addItem(item.getId(), count);

    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    System.out.println("Bought " + count + "x " + itemId + " for " + totalCost + ".");
  }

  private ShopItem findItem(String itemId) {
    for (ShopItem item : shop.getAllTimeProducts()) {
      if (item.getId().equals(itemId)) {
        return item;
      }
    }
    for (ShopItem item : shop.getDailyTimeProducts()) {
      if (item.getId().equals(itemId)) {
        return item;
      }
    }
    return null;
  }

  @Override
  public void exit() {
    System.out.println("Changed to Game Menu.");
    App.setCurrentMenu(Menu.GameMenu);
  }
}
