package model.plant;

abstract public class BasePlant {
    private int health;
    private int cost;
    // از اخرین اکتی که انجام داده شده زمان گذشته شده شو ذخیره بکنیم
    // داخل ابزرور باید همه این هارو اپدیت بکنیم
    // داخل زامبی هم همین و اعمال میکنیم
    // هلث و کاست و باید از جیسون گرفته بشه
    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public abstract void act();
}
