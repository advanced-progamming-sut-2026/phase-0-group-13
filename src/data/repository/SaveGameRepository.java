package data.repository;

import java.util.List;

public class SaveGameRepository implements CrudRepository<Object> {
    private final List<Object> saves;

    public SaveGameRepository(List<Object> saves) {
        this.saves = saves;
    }

    @Override
    public void save(Object o) {
        saves.add(o);
    }

    @Override
    public void update(Object o) {
    }

    @Override
    public void delete(Object o) {
        saves.remove(o);
    }


    @Override
    public Object find(String alias) {
        return null;
    }

    @Override
    public Object getAll() {
        return null;
    }
}
