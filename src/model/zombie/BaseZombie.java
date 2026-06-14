package model.zombie;

public class BaseZombie {
    private String type;
    private int health;
    private double speed;
    // دیزاین پترن bridge

    public BaseZombie() {
    }

    public void move() {
    }

    public void attack() {
    }

    public void takeDamage() {
    }

    public String getType() {
        return type;
    }

    public int getHealth() {
        return health;
    }

    public double getSpeed() {
        return speed;
    }

    protected void setType(String type) {
        this.type = type;
    }

    protected void setHealth(int health) {
        this.health = health;
    }

    protected void setSpeed(double speed) {
        this.speed = speed;
    }
}
