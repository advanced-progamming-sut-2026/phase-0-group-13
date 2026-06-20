package data.repository;

import model.game.zombie.ZombieParts.ZombieTemplate;
import java.util.List;

public class ZombieRepository implements ReadOnlyRepository<Object> {
    private final List<ZombieTemplate> zombies;

    public ZombieRepository(List<ZombieTemplate> zombies) {
        this.zombies = zombies;
    }

    public List<ZombieTemplate> getAll() {
        return zombies;
    }

    public ZombieTemplate find(String name) {
        for (ZombieTemplate template : zombies) {
            if (template.name.equalsIgnoreCase(name)) {
                return template;
            }
        }
        return null;
    }
}