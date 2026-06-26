package model.game.reward;

public class Unlockable {
  private String targetId;
  private boolean unlocked;

  public Unlockable() {
    this.unlocked = false;
  }

  public Unlockable(String targetId) {
    this.targetId = targetId;
    this.unlocked = false;
  }

  public void unlock() {
    unlocked = true;
  }
}
