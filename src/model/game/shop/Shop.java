package model.game.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.Result;
import model.account.User;
import model.enums.CurrencyType;
import model.enums.ItemCategory;

public class Shop {
  private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
  private static final long MILLIS_PER_DAY = 86400000L;
  private static final int DAILY_DEAL_COUNT = 3;

  private final List<ShopItem> allTimeProducts;
  private final Map<String, List<ShopItem>> userDailyDeals;
  private final Random random = new Random();

  public Shop() {
    this.allTimeProducts = new ArrayList<>();
    this.userDailyDeals = new HashMap<>();
    addAllTimeProducts();
  }

  public void addAllTimeProducts() {
    allTimeProducts.add(
            new ShopItem("pot_1", 2000, CurrencyType.COIN, -1, ItemCategory.POT, null, false));
    allTimeProducts.add(
            new ShopItem("food_1", 3, CurrencyType.DIAMOND, -1, ItemCategory.PLANT_FOOD, null, false));
    allTimeProducts.add(
            new ShopItem(
                    "rand_seed", 1000, CurrencyType.COIN, -1, ItemCategory.RANDOM_SEED, null, false));
    allTimeProducts.add(
            new ShopItem(
                    "cust_seed", 5, CurrencyType.DIAMOND, -1, ItemCategory.CUSTOM_SEED, null, false));
    allTimeProducts.add(
            new ShopItem(
                    "currency",
                    5,
                    CurrencyType.DIAMOND,
                    -1,
                    ItemCategory.CURRENCY_CONVERSION,
                    null,
                    false));
  }

  public List<ShopItem> getAllTimeProducts() {
    return allTimeProducts;
  }

  public List<ShopItem> getDailyTimeProducts(User user) {
    return userDailyDeals.getOrDefault(user.getUsername(), new ArrayList<>());
  }

  public void refreshDailyDealsIfNeeded(User user) {
    if (user == null) return;

    long now = System.currentTimeMillis();
    long last = user.getLastShopRefreshTime();

    if (!userDailyDeals.containsKey(user.getUsername())
            || last == 0
            || (now - last) >= ONE_DAY_MILLIS) {
      generateDailyDeals(user, now);
      if ((now - last) >= ONE_DAY_MILLIS || last == 0) {
        user.setLastShopRefreshTime(now);
      }
    }
  }

  private void generateDailyDeals(User user, long currentTime) {
    List<ShopItem> daily = new ArrayList<>();
    List<String> unlocked = user.getUnlockedPlants();

    long dayId = currentTime / ONE_DAY_MILLIS;
    Random dailyRandom = new Random(user.getUsername().hashCode() + dayId);

    if (unlocked == null || unlocked.isEmpty()) {
      daily.add(
              new ShopItem(
                      "daily_seed_peashooter",
                      1600,
                      CurrencyType.COIN,
                      1,
                      ItemCategory.RANDOM_SEED,
                      null,
                      true));
    } else {
      List<String> pool = new ArrayList<>(unlocked);
      for (int i = 0; i < DAILY_DEAL_COUNT && !pool.isEmpty(); i++) {
        int pickIndex = dailyRandom.nextInt(pool.size());
        String chosen = pool.remove(pickIndex);
        String itemId = "daily_seed_" + chosen;
        daily.add(
                new ShopItem(itemId, 1600, CurrencyType.COIN, 1, ItemCategory.RANDOM_SEED, null, true));
      }
    }
    userDailyDeals.put(user.getUsername(), daily);
  }

  public Result buyItem(User user, String itemId, int count, String plantTypeParam) {
    if (user == null) return new Result(false, "error: no user logged in", null);
    if (count <= 0) return new Result(false, "error: count must be positive", null);

    ShopItem item = findItem(user, itemId);
    if (item == null) return new Result(false, "error: item not found", null);
    if (!item.isAvailable()) return new Result(false, "error: item out of stock", null);

    Result dailyCheck = checkDailyDealNotAlreadyBought(user, item);
    if (!dailyCheck.success()) return dailyCheck;

    int totalCost = item.getPrice() * count;
    if (item.getCurrencyType() == CurrencyType.COIN) {
      if (user.getCoins() < totalCost) return new Result(false, "error: not enough coins", null);
    } else {
      if (user.getDiamonds() < totalCost)
        return new Result(false, "error: not enough diamonds", null);
    }

    Result effectResult = applyPurchaseEffect(user, item, count, plantTypeParam);
    if (!effectResult.success()) return effectResult;

    if (item.getCurrencyType() == CurrencyType.COIN) {
      user.addCoins(-totalCost);
    } else {
      user.addDiamonds(-totalCost);
    }

    for (int i = 0; i < count; i++) item.decreaseStock();

    if (item.isDaily()) {
      user.setLastDailyDealPurchaseTime(System.currentTimeMillis());
    }

    return new Result(true, effectResult.message(), item);
  }


  private Result checkDailyDealNotAlreadyBought(User user, ShopItem item) {
    if (!item.isDaily()) {
      return new Result(true, "ok", null);
    }
    long today = System.currentTimeMillis() / MILLIS_PER_DAY;
    long lastPurchaseDay = user.getLastDailyDealPurchaseTime() / MILLIS_PER_DAY;
    if (today == lastPurchaseDay) {
      return new Result(false, "error: you already bought today's daily deal", null);
    }
    return new Result(true, "ok", null);
  }

  private Result applyPurchaseEffect(User user, ShopItem item, int count, String plantTypeParam) {
    switch (item.getCategory()) {
      case POT:
        if (!user.getInventory().canAdd("pot", count)) {
          return new Result(
                  false, "error: greenhouse already has the maximum number of pots", null);
        }
        for (int i = 0; i < count; i++) user.getGreenHouse().unlockNextPot();
        user.getInventory().addItem("pot", count);
        return new Result(true, "Unlocked " + count + " new pot(s) in your greenhouse!", null);

      case PLANT_FOOD:
        if (!user.getInventory().canAdd("plant_food", count)) {
          return new Result(false, "error: you cannot hold more than 3 plant foods", null);
        }
        user.getInventory().addItem("plant_food", count);
        return new Result(true, "Bought " + count + " plant food(s).", null);

      case CURRENCY_CONVERSION:
        int coinsGained = count * 500;
        user.addCoins(coinsGained);
        return new Result(true, "Converted diamonds into " + coinsGained + " coins.", null);

      case RANDOM_SEED:
        String seedId = pickRandomSeedId(item, user);
        if (seedId == null)
          return new Result(false, "error: no unlocked plants available to grant", null);
        user.getInventory().addItem("seed_" + seedId, count * 10);
        return new Result(
                true, "Bought " + count + "x seed pack(s) (10 seeds each) for: " + seedId, null);

      case CUSTOM_SEED:
        if (plantTypeParam == null || plantTypeParam.trim().isEmpty()) {
          return new Result(false, "error: you must specify a plant with -t <plantType>", null);
        }
        String customName = plantTypeParam.trim().toLowerCase();
        if (!user.hasUnlockedPlant(customName)) {
          return new Result(false, "error: you have not unlocked " + customName + " yet", null);
        }
        user.getInventory().addItem("seed_" + customName, count * 10);
        return new Result(
                true, "Bought " + count + "x seed pack(s) (10 seeds each) for: " + customName, null);

      default:
        return new Result(false, "error: unknown item category", null);
    }
  }

  private String pickRandomSeedId(ShopItem item, User user) {
    if (item.getId().startsWith("daily_seed_")) {
      return item.getId().substring("daily_seed_".length());
    }
    List<String> unlocked = user.getUnlockedPlants();
    if (unlocked == null || unlocked.isEmpty()) return null;
    return unlocked.get(random.nextInt(unlocked.size()));
  }

  private ShopItem findItem(User user, String itemId) {
    for (ShopItem shopItem : allTimeProducts) {
      if (shopItem.getId().equals(itemId)) return shopItem;
    }
    List<ShopItem> daily = getDailyTimeProducts(user);
    for (ShopItem shopItem : daily) {
      if (shopItem.getId().equals(itemId)) return shopItem;
    }
    return null;
  }
}