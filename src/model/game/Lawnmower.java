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
    this.x = -0.5; // شروع از پشت گیاها
    this.isActive = true;
    this.isTriggered = false;
  }

  public void trigger() {
    if (isActive && !isTriggered) {
      this.isTriggered = true;
    }
  }

  /** Rolls one tick and returns whatever it ran over, so the board can credit the kills. */
  public List<Zombie> move(List<Zombie> zombies) {
    if (!isTriggered || !isActive) {
      return List.of();
    }
    this.x += 0.25; // سرعت حرکت چمن‌زن در هر تیک

    List<Zombie> crushed = new ArrayList<>();
    // نابود کردن زامبی‌های در مسیر خط افقی ردیف
    for (Zombie zombie : zombies) {
      if (zombie.isBoss()) {
        continue; // a mower rolls straight under the robot
      }
      if (zombie.occupiesRow(this.row) && !zombie.isDead()
              && Math.abs(zombie.getX() - this.x) < 0.8) {
        zombie.takeDamage(10000, true); // یچی میدم بده مثل بمب صدا
        crushed.add(zombie);
      }
    }

    // خروج از میدان نبرد
    if (this.x > 10.0) {
      this.isActive = false;
      this.isTriggered = false;
    }
    return crushed;
  }

  /** Still parked at the left, so this row is still protected. */
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
