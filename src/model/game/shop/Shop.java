package model.game.shop;

import model.account.User;
import model.enums.CurrencyType;
import model.enums.ItemCategory;

import java.util.ArrayList;
import java.util.List;

public class Shop {
  private List<ShopItem> allTimeProducts;
  private List<ShopItem> dailyTimeProducts;

  public Shop() {
    this.allTimeProducts = new ArrayList<>();
    this.dailyTimeProducts = new ArrayList<>();
    addAllTimeProducts();
    addDailyProducts();
  }

  public void addAllTimeProducts() {
    allTimeProducts.add(new ShopItem("pot_1",
            2000, CurrencyType.COIN, 20, ItemCategory.POT, null, false));
    allTimeProducts.add(new ShopItem("food_1",
            3, CurrencyType.DIAMOND, 3, ItemCategory.PLANT_FOOD, null, false));
    allTimeProducts.add(new ShopItem("rand_seed",
            1000, CurrencyType.COIN, -1, ItemCategory.RANDOM_SEED, null, false));
    allTimeProducts.add(new ShopItem("cust_seed",
            5, CurrencyType.DIAMOND, -1, ItemCategory.CUSTOM_SEED, null, false));
    allTimeProducts.add(new ShopItem("currency",
            5, CurrencyType.DIAMOND, -1, ItemCategory.CURRENCY_CONVERSION, null, false));
  }

  public void addDailyProducts() {
    dailyTimeProducts.clear();
    dailyTimeProducts.add(new ShopItem("daily_seed", 1600, CurrencyType.COIN, 1, ItemCategory.RANDOM_SEED, null, true));
  }

  public void buyItem(User currentUser, String itemId, int count, String plantTypeStr) {
    // تو اینجا وقتی یارو یچی میخره باید این و صدا بزنیم که هندل بکنه کاری که میخوایم و

  }
}