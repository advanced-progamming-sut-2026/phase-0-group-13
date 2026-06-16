package model.game.zombie.ZombieParts;

public class ZombieArmorProps {
    public String type;
    public int armorHealth;

    public ZombieArmorProps cloneProps() {
        ZombieArmorProps copy = new ZombieArmorProps();
        copy.type = this.type;
        copy.armorHealth = this.armorHealth;
        return copy;
    }
}
