package model.game;

import model.enums.SunType;

public class Sun {
    private int amount;
    private int xCoordinate;
    private int yCoordinate;
    private int timeToLive;
    private SunType sunType;
    //amount باید داخل suntype باشد
    //مختصات و داخل خود ابجکت ها ذخیره بکنیم ( یه اینترفیس داشته باشیم که این و هندل بکنه )

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
