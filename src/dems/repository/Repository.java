package dems.repository;

import dems.model.Identifiable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * GENERIC INTERFACE: the "database" contract. Works for any entity T that has an id of type ID.
 * The rest of the application only depends on this interface, so the storage
 * (text files today, a SQL database tomorrow) can be swapped without touching the services.
 */
public interface Repository<T extends Identifiable<ID>, ID> {

    void save(T item) throws IOException;

    void saveAll(Collection<T> items) throws IOException;

    Optional<T> findById(ID id);

    List<T> findAll();

    /** Lambda-friendly search: repo.findWhere(c -> c.getStatus() == CaseStatus.OPEN). */
    List<T> findWhere(Predicate<T> filter);

    boolean delete(ID id) throws IOException;

    boolean exists(ID id);
}
