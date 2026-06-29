package model.game.reward;

import model.account.User;

public class Unlockable implements Reward {

  private final String alias;

  public Unlockable(String targetId) {
    this.alias = targetId;
  }

  @Override
  public void apply(User user) {
    user.unlockItem(alias);
  }

  public String getAlias() { return alias; }
}