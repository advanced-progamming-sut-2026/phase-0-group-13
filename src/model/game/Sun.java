package model.game;

import model.enums.SunType;

public class Sun {
    private int amount;
    private double x;
    private double y;
    private int timeToLive; // به واحد تیک
    private SunType sunType;
    private boolean isCollected;

    public Sun(int amount, int timeToLive, SunType sunType) {
        this.amount = amount;
        this.timeToLive = timeToLive;
        this.sunType = sunType;
        this.x = 0.0;
        this.y = 0.0;
        this.isCollected = false;
    }

    public void changinCordinate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public void update(int currentTick) {
        if (timeToLive > 0 && !isCollected) {
            timeToLive--;
        }
    }

    public void collect(GameState state) {
        if (!isCollected && timeToLive > 0) {
            this.isCollected = true;
            state.addSun(this.amount);
            this.timeToLive = 0; // تیک بعدی پخ پخ شه
        }
    }

    public boolean isExpired() {
        return timeToLive <= 0 || isCollected;
    }

    public double getX() { return x; }
    public double getY() { return y; }
}