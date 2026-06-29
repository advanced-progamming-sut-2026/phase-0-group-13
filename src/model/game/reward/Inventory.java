package model.game.reward;

import model.account.User;

public class Inventory implements Reward {
  private final String itemId;
  private final int quantity;
  public Inventory(String itemId, int quantity) {
    this.itemId = itemId;
    this.quantity = quantity;
  }

  @Override
  public void apply(User user) {
    user.getInventory().addItem(itemId, quantity);
  }

  public String getItemId() { return itemId; }
  public int getQuantity() { return quantity; }
}