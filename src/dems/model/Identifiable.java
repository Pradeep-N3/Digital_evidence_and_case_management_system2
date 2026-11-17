package dems.model;

/**
 * INTERFACE: anything that can be stored in a repository has a unique id.
 *
 * @param <ID> type of the identifier (String in this project)
 */
public interface Identifiable<ID> {
    ID getId();
}
