package engine.behavior;

/**
 * Defines a generalized hash function that can be implemented to add custom hashing functions
 * and use in hashed data structures
 * @param <T>
 */
@FunctionalInterface
public interface Hasher<T> {
    int hashcode(T item);
}
