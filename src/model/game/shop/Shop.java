package model.game.shop;

import java.util.List;
import java.util.Objects;

public class Shop {
  private List<Objects> allTimeProducts;
  private List<Objects> dailyTimeProducts;

  public void listItems() {}

  public void buyItem(String itemId, int count, String plantType) {}

  public List<Objects> getAllTimeProducts() {
    return allTimeProducts;
  }

  public void addAllTimeProducts() {}

  public void addDailyProducts() {}
}
