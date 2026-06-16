package model.game;

import model.game.zombie.Zombie;

public class Projectile {
    private int damage;
    private int speed;
    private int xCoordinate;
    private int yCoordinate;
    private boolean isSlowing;

    public Projectile(int damage, int speed, int x, int y, boolean isSlowing) {
        this.damage = damage;
        this.speed = speed;
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.isSlowing = isSlowing;
    }

    public void move() {
    }

    public void hit(Zombie zombie) {

    }
    public void destroyZombie(){}
    public void destroyPlant(){}
}
