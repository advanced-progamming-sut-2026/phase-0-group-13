package model.zombie.Factory;

import data.repository.ZombieRepository;
import model.zombie.Zombie;
import model.zombie.ZombieParts.ZombieTemplate;

public class ZombieFactory {
    private ZombieRepository repository;

    public ZombieFactory(ZombieRepository repository) {
        this.repository = repository;
    }

    public Zombie createZombie(String alias) {
        ZombieTemplate template = repository.find(alias);
        return new Zombie(template);
    }
}
