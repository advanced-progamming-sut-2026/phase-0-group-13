package model.game.zombie;

import model.enums.StatusEffect;
import model.game.Board;
import model.game.zombie.ZombieParts.Armor;
import model.game.zombie.behavior.ZombieAction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Zombie {
    private String name;
    private int currentHealth;
    private int maxHealth;
    private double speed;

    private int row;
    private double x;
    private double y;

    private List<Armor> armors;
    private ZombieAction behavior;

    private boolean isEating;
    private Map<StatusEffect, Integer> activeEffects;

    public Zombie(String name, int health, double speed, int row, double startX, ZombieAction behavior) {
        this.name = name;
        this.maxHealth = health;
        this.currentHealth = health;
        this.speed = speed;
        this.row = row;
        this.x = startX;

        this.armors = new ArrayList<>();
        this.behavior = behavior;
        this.isEating = false;
        this.activeEffects = new EnumMap<>(StatusEffect.class);
    }

    public void addArmor(Armor armor) {
        if (armor != null) {
            this.armors.add(armor);
        }
    }

    public void update(int currentTick, Board board) {
        if (isDead()) return;

        processEffects();

        if (!activeEffects.containsKey(StatusEffect.FROZEN)) {
            if (behavior != null) {
                behavior.execute(this, board, currentTick);
            }
        }
    }

    private void processEffects() {
        Iterator<Map.Entry<StatusEffect, Integer>> it = activeEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<StatusEffect, Integer> entry = it.next();
            int remainingTicks = entry.getValue() - 1;

            if (remainingTicks <= 0) {
                it.remove();
            } else {
                activeEffects.put(entry.getKey(), remainingTicks);
            }

            if (entry.getKey() == StatusEffect.POISONED) {
                this.currentHealth -= 2;
            }
        }
    }

    public void move() {
        if (!isEating && !activeEffects.containsKey(StatusEffect.FROZEN)) {
            double actualSpeed = activeEffects.containsKey(StatusEffect.CHILLED) ? speed / 2.0 : speed;
            this.x -= actualSpeed;
        }
    }

    public void takeDamage(int damage, boolean ignoresArmor) {
        if (ignoresArmor) {
            this.currentHealth -= damage;
        } else {
            int remainingDamage = damage;
            // فیلتر کردن دمیج از طریق لایه‌های زره
            for (Armor armor : armors) {
                if (!armor.isDestroyed()) {
                    remainingDamage = armor.takeDamage(remainingDamage);
                    if (remainingDamage <= 0) break;
                }
            }
            if (remainingDamage > 0) {
                this.currentHealth -= remainingDamage;
            }
        }
        this.currentHealth = Math.max(0, this.currentHealth);
    }

    public void applyEffect(StatusEffect effect, int durationInTicks) {
        this.activeEffects.put(effect, durationInTicks);
    }

    public boolean isDead() { return this.currentHealth <= 0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getRow() { return row; }
    public String getName() { return name; }
    public int getCurrentHealth() { return currentHealth; }
    public boolean isEating() { return isEating; }
    public void setEating(boolean eating) { this.isEating = eating; }
}