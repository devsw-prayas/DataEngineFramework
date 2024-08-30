package data.core;

import java.util.Iterator;

/**
 * A list-iterator that allows for bidirectional traversals and replacements. All lists are recommended to provide
 * an implementation that runs concurrently on the underlying array or other data storage to allow various predefined
 * algorithms to run efficiently.
 * @param <E> Type argument of data
 *
 * @author devsw
 */
public interface ListIterator<E> extends Iterator<E>{

    /**
     * Checks if an item is present before the current item
     */
    boolean hasPrevious();

    /**
     * Returns the previous item present before the current item
     */
    E previous();

    /**
     * Sets the current item to the given {@code item}
     */
    void set(E item);
}
