package model.game;

import java.util.ArrayList;
import java.util.List;
import model.game.zombie.Zombie;

public class Lawnmower {
  private int row;
  private double x;
  private boolean isActive;
  private boolean isTriggered;

  public Lawnmower(int row) {
    this.row = row;
    this.x = -0.5;
    this.isActive = true;
    this.isTriggered = false;
  }

  public void trigger() {
    if (isActive && !isTriggered) {
      this.isTriggered = true;
    }
  }

  public List<Zombie> move(List<Zombie> zombies) {
    if (!isTriggered || !isActive) {
      return List.of();
    }
    this.x += 0.25;

    List<Zombie> crushed = new ArrayList<>();
    for (Zombie zombie : zombies) {
      if (zombie.isBoss()) {
        continue;
      }
      if (zombie.occupiesRow(this.row) && !zombie.isDead()
              && Math.abs(zombie.getX() - this.x) < 0.8) {
        zombie.takeDamage(10000, true);
        crushed.add(zombie);
      }
    }

    if (this.x > 10.0) {
      this.isActive = false;
      this.isTriggered = false;
    }
    return crushed;
  }

  public boolean isAvailable() {
    return isActive && !isTriggered;
  }

  public int getRow() {
    return row;
  }

  public double getX() {
    return x;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isTriggered() {
    return isTriggered;
  }

  public void setActive(boolean active) {
    isActive = active;
  }
}
