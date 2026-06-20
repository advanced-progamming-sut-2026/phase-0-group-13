package model.game;

import model.game.zombie.Zombie;
import java.util.List;

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

    public void move(List<Zombie> zombies) {
        if (isTriggered && isActive) {
            this.x += 0.25; // سرعت حرکت چمن‌زن در هر تیک

            // نابود کردن زامبی‌های در مسیر خط افقی ردیف
            for (Zombie zombie : zombies) {
                if (zombie.getRow() == this.row && Math.abs(zombie.getX() - this.x) < 0.8) {
                    zombie.takeDamage(10000, true); // یچی میدم بده مثل بمب صدا
                }
            }

            // خروج از میدان نبرد
            if (this.x > 10.0) {
                this.isActive = false;
                this.isTriggered = false;
            }
        }
    }

    public int getRow() { return row; }
    public boolean isActive() { return isActive; }
    public boolean isTriggered() { return isTriggered; }
    public void setActive(boolean active) { isActive = active; }
}