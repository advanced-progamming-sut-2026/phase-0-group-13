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
    @Override
    public ZombieTemplate find(String alias) {
        for (ZombieTemplate template : zombies) {
            if (template.alias.equalsIgnoreCase(alias)) {
                return template;
            }
        }
        return null;
    }

}
