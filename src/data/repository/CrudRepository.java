package data.repository;

public interface CrudRepository<Type> extends ReadOnlyRepository {
    void save(Type type);

    void update(Type type);

    void delete(Type type);
}
