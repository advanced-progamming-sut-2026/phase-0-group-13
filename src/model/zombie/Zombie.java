package model.zombie;

import model.zombie.ZombieParts.ZombieArmorProps;
import model.zombie.ZombieParts.ZombieTemplate;

public class Zombie {
    private String name;
    private int currentHealth;
    private double currentSpeed;
    private int eatDps;
    private ZombieArmorProps armor;
    private double x ;
    private double y ;

    public Zombie(ZombieTemplate template) {
        this.name = template.name;
        this.currentHealth = template.stats.baseHealth;
        this.currentSpeed = template.stats.speed;
        this.eatDps = template.stats.eatDps;
        this.x = 0.0;
        this.y = 0.0;
        if (template.armor != null) {
            this.armor = template.armor.cloneProps();
        }
    }
    public void changinCordinate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public void takeDamage(int damage) {
        if (armor != null && armor.armorHealth > 0) {
            armor.armorHealth -= damage;
            if (armor.armorHealth <= 0) {
                armor = null;
            }
        } else {
            currentHealth -= damage;
        }
    }

}

