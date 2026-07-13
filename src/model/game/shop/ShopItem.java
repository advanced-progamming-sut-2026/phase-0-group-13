package model.game.shop;

import model.enums.CurrencyType;
import model.enums.ItemCategory;
import model.enums.PlantType;

public class ShopItem {

  private final String id;
  private final int price;
  private final CurrencyType currencyType;
  private final ItemCategory category;
  private final PlantType plantType;
  private final boolean daily;
  private int stock;

  public ShopItem(
          String id,
          int price,
          CurrencyType currencyType,
          int stock,
          ItemCategory category,
          PlantType plantType,
          boolean daily) {
    this.id = id;
    this.price = price;
    this.currencyType = currencyType;
    this.stock = stock;
    this.category = category;
    this.plantType = plantType;
    this.daily = daily;
  }

  public int getPrice() {
    return price;
  }

  public String getId() {
    return id;
  }

  public int getStock() {
    return stock;
  }

  public CurrencyType getCurrencyType() {
    return currencyType;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public boolean isAvailable() {
    return stock > 0 || stock == -1;
  }

  public boolean isDaily() {
    return daily;
  }

  public void decreaseStock() {
    if (stock > 0) stock--;
  }
}