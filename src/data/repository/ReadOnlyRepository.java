package data.repository;

public interface ReadOnlyRepository<Type> {
    Type find(String alias);
    Type getAll();
}
