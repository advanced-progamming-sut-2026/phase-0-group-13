package model.game.reward;

import model.account.User;

public record Currency(String currencyType, int amount) implements Reward {
  @Override
  public void apply(User user) {
    if ("COIN".equalsIgnoreCase(currencyType)) {
      user.addCoins(amount);
    } else if ("DIAMOND".equalsIgnoreCase(currencyType)) {
      user.addDiamonds(amount);
    }
  }


}