package model.game;

import model.enums.StatusEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class Projectile {
    private int damage;
    private double speed;
    private double xCoordinate;
    private int yCoordinate;
    private boolean isSlowing;
    private boolean isFromZombie;
    private boolean isActive;

    public Projectile(int damage, double speed, double x, int y, boolean isSlowing, boolean isFromZombie) {
        this.damage = damage;
        this.speed = speed;
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.isSlowing = isSlowing;
        this.isFromZombie = isFromZombie;
        this.isActive = true;
    }

    public void move() {
        if (isActive) {
            this.xCoordinate += speed;
        }
    }

    public void hitZombie(Zombie zombie) {
        if (!isActive || isFromZombie) return;

        zombie.takeDamage(this.damage, false);
        if (isSlowing) {
            zombie.applyEffect(StatusEffect.CHILLED, 50); // اعمال افکت سرما برای ۵۰ تیک
        }
        this.isActive = false;
    }

    public void hitPlant(Plant plant) {
        if (!isActive || !isFromZombie) return;

        plant.takeDamage(this.damage);
        this.isActive = false;
    }

    public double getXCoordinate() { return xCoordinate; }
    public int getYCoordinate() { return yCoordinate; }
    public boolean isFromZombie() { return isFromZombie; }
    public boolean isActive() { return isActive; }
}