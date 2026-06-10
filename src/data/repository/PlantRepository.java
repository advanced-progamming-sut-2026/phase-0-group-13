package data.repository;

import java.util.List;

public class PlantRepository implements ReadOnlyRepository {
    @Override
    public Object findById(int id) {
        return null;
    }

    @Override
    public List findAll() {
        return List.of();
    }
}
