package data.repository;

import java.util.List;

public class ZombieRepository implements ReadOnlyRepository<Object> {
    private final List<Object> zombies;

    public ZombieRepository(List<Object> zombies) {
        this.zombies = zombies;
    }

    @Override
    public Object findById(int id) {
        return null;
    }

    @Override
    public List<Object> findAll() {
        return zombies;
    }
}
