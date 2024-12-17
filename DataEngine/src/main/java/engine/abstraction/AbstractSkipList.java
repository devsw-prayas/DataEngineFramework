package engine.abstraction;

import data.constants.ImplementationType;
import data.core.AbstractDataEngine;
import data.core.DataEngine;
import data.core.ImmutableException;
import data.core.Implementation;
import engine.behavior.Hasher;

/**
 * The top level superclass for all skip-lists. A skip-list is a data structure that allows for
 * logarithmic search time. This abstraction defines all the behavior common to all possible forms
 * of a skip-list. Any concurrent behavior is to be added manually per implementation. A skip
 * list is a data structure that acts as a multi-level linked list. Various methods from
 * {@link DataEngine} are disabled, instead alternative methods are provided.
 * <p></p>
 * This is a specialized abstraction, as it is not a direct subclass of {@link AbstractDataEngine}.
 *
 * @param <E> Type argument of data being stored
 */
@Implementation(value = ImplementationType.ABSTRACTION, isSpecialized = true)
public abstract class AbstractSkipList<E> implements DataEngine<E> {

    // Removing support for invalid methods for a skip-list

    @Override
    public int getActiveSize() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public int getMaxCapacity() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public E[] toArray(int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public E[] toArray(int start) {
        throw new UnsupportedOperationException("Method not supported");
    }

    private Hasher<E> hasher; //The required hashing function

}
