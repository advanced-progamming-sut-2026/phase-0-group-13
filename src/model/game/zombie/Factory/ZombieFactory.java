package model.game.zombie.Factory;

import data.repository.ZombieRepository;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;

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
