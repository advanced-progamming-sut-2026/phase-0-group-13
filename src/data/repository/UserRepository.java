package data.repository;

import java.util.List;

public class UserRepository implements CrudRepository<Object> {
    private final List<Object> users;

    public UserRepository(List<Object> users) {
        this.users = users;
    }

    @Override
    public void save(Object o) {
        users.add(o);
    }

    @Override
    public void update(Object o) {
    }

    @Override
    public void delete(Object o) {
        users.remove(o);
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
