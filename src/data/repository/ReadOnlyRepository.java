package data.repository;

import java.util.List;

public interface ReadOnlyRepository<Type> {
    Type findById(int id);

    List<Type> findAll();
}
