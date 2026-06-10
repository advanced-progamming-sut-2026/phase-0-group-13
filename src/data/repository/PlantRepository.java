package data.repository;

import java.util.List;

public class PlantRepository implements ReadOnlyRepository<Object> {
    private final List<Object> plants;

    public PlantRepository(List<Object> plants) {
        this.plants = plants;
    }

    @Override
    public Object findById(int id) {
        return null;
    }

    @Override
    public List<Object> findAll() {
        return plants;
    }
}
