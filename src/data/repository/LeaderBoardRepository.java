package data.repository;

import java.util.List;

public class LeaderBoardRepository implements CrudRepository<Object> {
    private final List<Object> entries;

    public LeaderBoardRepository(List<Object> entries) {
        this.entries = entries;
    }

    @Override
    public void save(Object o) {
        entries.add(o);
    }

    @Override
    public void update(Object o) {
    }

    @Override
    public void delete(Object o) {
        entries.remove(o);
    }

    @Override
    public Object findById(int id) {
        return null;
    }

    @Override
    public List<Object> findAll() {
        return entries;
    }
}
