package model;

import model.enums.SunType;

public class Sun {
    private int amount;
    private int xCoordinate;
    private int yCoordinate;
    private int timeToLive;
    private SunType sunType;

    public Sun(int amount, int x, int y, int timeToLive, SunType sunType) {
        this.amount = amount;
        this.sunType = sunType;
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.timeToLive = timeToLive;
    }

    public void collect() {
    }

    public boolean isExpired() {
        return timeToLive <= 0;
    }
}
