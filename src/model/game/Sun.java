package model.game;

import model.enums.SunType;

public class Sun {
    private int amount;
    private double x;
    private double y;
    private int timeToLive;
    private SunType sunType;
    //amount باید داخل suntype باشد
    //مختصات و داخل خود ابجکت ها ذخیره بکنیم ( یه اینترفیس داشته باشیم که این و هندل بکنه )

    public Sun(int amount, int timeToLive, SunType sunType) {
        this.amount = amount;
        this.sunType = sunType;
        this.x = 0.0;
        this.y = 0.0;
        this.timeToLive = timeToLive;
    }
    public void changinCordinate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public void collect() {
    }

    public boolean isExpired() {
        return timeToLive <= 0;
    }
}
