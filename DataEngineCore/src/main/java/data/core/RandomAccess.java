package data.core;

/**
 * A special marker interface. Indicates that a data-engine has an underlying array. This means any random
 * accessing of elements is much faster as compared to node-based engines. It is highly recommended that all
 * data-engines using an underlying array should be marked with this. Exceptions to this case are when the
 * engine provides multithreading support using internal locking, segmented striping or atomic behavior.
 * In such cases any contention caused due to this could slow down the application. Thus, for such cases
 * this marker is not recommended, mostly because it is guaranteed that versions for thread-safe forms will be
 * available
 *
 * @see EngineNature
 * @author devsw
 *
 */
public interface RandomAccess {}