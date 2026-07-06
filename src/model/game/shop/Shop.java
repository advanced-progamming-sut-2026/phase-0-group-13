package model.game.shop;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.Result;
import model.account.User;
import model.enums.CurrencyType;
import model.enums.ItemCategory;
import model.game.plant.PlantParts.PlantTemplate;

public class Shop {
  private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
  private static final int DIAMOND_TO_COIN_RATE = 100;
  private static final int DAILY_DEAL_COUNT = 3;

  private final List<ShopItem> allTimeProducts;
  private final List<ShopItem> dailyTimeProducts;
  private final Random random = new Random();

  public Shop() {
    this.allTimeProducts = new ArrayList<>();
    this.dailyTimeProducts = new ArrayList<>();
    addAllTimeProducts();
    addDailyProducts();
  }

  public void addAllTimeProducts() {
    allTimeProducts.add(
        new ShopItem("pot_1", 2000, CurrencyType.COIN, 20, ItemCategory.POT, null, false));
    allTimeProducts.add(
        new ShopItem("food_1", 3, CurrencyType.DIAMOND, 3, ItemCategory.PLANT_FOOD, null, false));
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

  public void addDailyProducts() {
    dailyTimeProducts.clear();
    dailyTimeProducts.add(
        new ShopItem(
            "daily_seed", 1600, CurrencyType.COIN, 1, ItemCategory.RANDOM_SEED, null, true));
  }

  public List<ShopItem> getAllTimeProducts() {
    return allTimeProducts;
  }

  public List<ShopItem> getDailyTimeProducts() {
    return dailyTimeProducts;
  }

  public void refreshDailyDealsIfNeeded(User user) {
    if (user == null) return;

    long now = System.currentTimeMillis();
    long last = user.getLastShopRefreshTime();

    if (last != 0 && (now - last) < ONE_DAY_MILLIS) {
      return;
    }

    generateDailyDeals();
    user.setLastShopRefreshTime(now);
  }

  private void generateDailyDeals() {
    dailyTimeProducts.clear();

    List<PlantTemplate> allPlants =
        GameDataManager.plantRepository != null ? GameDataManager.plantRepository.getAll() : null;

    if (allPlants == null || allPlants.isEmpty()) {
      addDailyProducts();
      return;
    }

    List<PlantTemplate> pool = new ArrayList<>(allPlants);
    for (int i = 0; i < DAILY_DEAL_COUNT && !pool.isEmpty(); i++) {
      int pickIndex = random.nextInt(pool.size());
      PlantTemplate chosen = pool.remove(pickIndex);

      int price = Math.max(100, chosen.cost * 10);
      String itemId = "daily_seed_" + chosen.name.toLowerCase().replace(' ', '_');

      dailyTimeProducts.add(
          new ShopItem(itemId, price, CurrencyType.COIN, 1, ItemCategory.RANDOM_SEED, null, true));
    }
  }

  public Result buyItem(User user, String itemId, int count, String plantTypeParam) {
    if (user == null) {
      return new Result(false, "error: no user logged in", null);
    }
    if (count <= 0) {
      return new Result(false, "error: count must be positive", null);
    }

    ShopItem item = findItem(itemId);
    if (item == null) {
      return new Result(false, "error: item not found", null);
    }
    if (!item.isAvailable()) {
      return new Result(false, "error: item out of stock", null);
    }

    int totalCost = item.getPrice() * count;
    if (item.getCurrencyType() == CurrencyType.COIN) {
      if (user.getCoins() < totalCost) {
        return new Result(false, "error: not enough coins", null);
      }
    } else {
      if (user.getDiamonds() < totalCost) {
        return new Result(false, "error: not enough diamonds", null);
      }
    }

    Result effectResult = applyPurchaseEffect(user, item, count, plantTypeParam);
    if (!effectResult.success()) {
      return effectResult;
    }

    if (item.getCurrencyType() == CurrencyType.COIN) {
      user.addCoins(-totalCost);
    } else {
      user.addDiamonds(-totalCost);
    }

    for (int i = 0; i < count; i++) {
      item.decreaseStock();
    }

    return new Result(true, effectResult.message(), item);
  }

  private Result applyPurchaseEffect(User user, ShopItem item, int count, String plantTypeParam) {
    switch (item.getCategory()) {
      case POT:
        for (int i = 0; i < count; i++) {
          if (!user.getGreenHouse().unlockNextPot()) {
            return new Result(
                false, "error: greenhouse already has the maximum number of pots", null);
          }
        }
        return new Result(true, "Unlocked " + count + " new pot(s) in your greenhouse!", null);

      case PLANT_FOOD:
        user.getInventory().addItem("plant_food", count);
        return new Result(true, "Bought " + count + " plant food.", null);

      case CURRENCY_CONVERSION:
        int coinsGained = count * DIAMOND_TO_COIN_RATE;
        user.addCoins(coinsGained);
        return new Result(
            true, "Converted " + count + " diamond(s) into " + coinsGained + " coins.", null);

      case RANDOM_SEED:
        {
          String seedId = pickRandomSeedId(item, user);
          if (seedId == null) {
            return new Result(false, "error: no seeds available to grant", null);
          }
          user.getInventory().addItem("seed_" + seedId, count);
          return new Result(true, "Bought " + count + "x seed pack: " + seedId, null);
        }

      case CUSTOM_SEED:
        if (plantTypeParam == null || plantTypeParam.trim().isEmpty()) {
          return new Result(false, "error: you must specify a plant with -t <plantType>", null);
        }
        PlantTemplate template =
            GameDataManager.plantRepository != null
                ? GameDataManager.plantRepository.find(plantTypeParam.trim())
                : null;
        if (template == null) {
          return new Result(false, "error: unknown plant type: " + plantTypeParam, null);
        }
        user.getInventory().addItem("seed_" + template.name.toLowerCase(), count);
        return new Result(true, "Bought " + count + "x seed pack: " + template.name, null);

      default:
        return new Result(false, "error: unknown item category", null);
    }
  }

  private String pickRandomSeedId(ShopItem item, User user) {

    if (item.getId().startsWith("daily_seed_")) {
      return item.getId().substring("daily_seed_".length());
    }

    List<PlantTemplate> allPlants =
        GameDataManager.plantRepository != null ? GameDataManager.plantRepository.getAll() : null;
    if (allPlants == null || allPlants.isEmpty()) {
      return null;
    }
    PlantTemplate chosen = allPlants.get(random.nextInt(allPlants.size()));
    return chosen.name.toLowerCase();
  }

  private ShopItem findItem(String itemId) {
    for (ShopItem shopItem : allTimeProducts) {
      if (shopItem.getId().equals(itemId)) {
        return shopItem;
      }
    }
    for (ShopItem shopItem : dailyTimeProducts) {
      if (shopItem.getId().equals(itemId)) {
        return shopItem;
      }
    }
    return null;
  }
}
